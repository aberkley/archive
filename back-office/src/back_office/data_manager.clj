(ns back-office.data-manager
  (:use [back-office.data-feed :as feed]
        clojure.set
        clojure.pprint
        [clojure.string :only [split trim]])
  (:require [entomic.api :as a]
            [db.core :as db]
            [clj-time.core :as t]
            [utilities.core :as u]
            [entomic.coerce :as c]))

(defn last-time
  "finds last time of a price in the db"
  [product]
  (->> (a/f {:price/product product})
       (map :price/time)
       sort
       last))

(defn update-price-data!
  [products f-prices]
  (let [end-date (u/date-adj (t/today-at 0 0) -1)
        prices (for [p products]
                 (try
                   (f-prices (last-time p) end-date [(db/bb-name-of p)])
                   (catch Exception e (throw (Exception. (str p))))))]
    (->> prices
         flatten
         (filter identity)
         (partition-all 100)
         (map a/update!))))

(defn update-product-data!
  [products f-reference]
  ;;TODO: check whether the value exists before downloading (again)
  (->> products
       (map db/bb-name-of)
       f-reference
       (filter identity)
       (partition-all 100)
       (map a/update!)))

(def bb-year
  (- (t/year (t/now)) 2000))

(defn bb-number
  [x]
  (cond
   (< x 10) (str 0 x)
   (>= x bb-year) (str (last (str x)))
   :else (str x)))

(defn bb-future-symbol
  [prefix letter number suffix]
  (str prefix letter (bb-number number) " " suffix))

(defn generic-bb-future-symbol
  [prefix suffix]
  (str prefix "A " suffix))

(defn all-bb-futures-symbols
  [prefix suffix letters]
  (for [y (range 19)]
    (for [l letters]
      (bb-future-symbol prefix l y suffix))))

(defn alpha-or-numeric-substring
  [a-not-n s]
  (->> s
       seq
       (map #(if (= (not a-not-n) (Character/isDigit %)) %))
       (filter identity)
       (apply str)))

(defn bb-future-symbol-components
  [symbol]
  (let [suffixes #{"Index" "Comdty" "Equity"}
        suffix (->> suffixes
                    (map #(re-find (re-pattern (str " " %)) symbol))
                    (filter identity)
                    first)
        stem (-> symbol
                 (split (re-pattern suffix))
                 first)
        number (Integer. (alpha-or-numeric-substring false stem))
        stem' (alpha-or-numeric-substring true stem)
        letter (last (seq stem'))
        prefix (apply str (butlast (seq stem')))]
    {:prefix prefix
     :letter letter
     :number number
     :suffix (trim suffix)}))

(defn futures-symbol-groups
  []
  (->> (a/f {:product/type "Future"})
       (map (fn [f] [f (db/bb-name-of f)]))
       (map (fn [[f n]]
              [f (bb-future-symbol-components n)]))
       (group-by (fn [[f c]] ((juxt :prefix :suffix) c)))))

(defn- get-futures-chains
  [products]
  (->> (feed/bulk-reference-data ["FUT_CHAIN"] [["Security Description"]] products)
       (map (fn [d] (map #(get % "Security Description") (get d "FUT_CHAIN"))))))

(defn save-futures-chain!
  [seed]
  (let [synonyms (->> seed
                      vector
                      get-futures-chains
                      flatten
                      (filter (complement #(a/fu {:synonym/name %})))
                      (filter (comp (partial > 19) :number bb-future-symbol-components))
                      (map (fn [s] {:synonym/name s
                                   :synonym/is-bb-name? true
                                   :synonym/type :synonym.type/ticker})))
        products (repeat (count synonyms) {:product/type "Future"})]
;;    [synonyms products]
    (db/save-new-products! synonyms products)))

(defn get-dual-or-single-priced
  [products]
  (let [smap {"LAST_PRICE" :last "FUND_NET_ASSET_VAL" :nav "PX_BID" :bid "PX_ASK" :ask "security" :product/name}
        lasts (feed/simple-reference-data ["LAST_PRICE"] products)
        navs (feed/simple-reference-data ["FUND_NET_ASSET_VAL"] products)
        bids (feed/simple-reference-data ["PX_BID"] products)
        asks (feed/simple-reference-data ["PX_ASK"] products)]
    (->> [navs lasts bids asks]
         (apply map merge)
         (map #(rename-keys % smap))
         (map (fn [{:keys [nav last bid ask] product-name :product/name}]
                {:product/product product-name
                 :product/single-priced? (boolean (and nav (not bid) (not ask)))})))))

(defn filter-existing-reference-data
  [fields products]
  (let [attributes (->> fields
                        (map (partial get db/bloomberg-attribute-map)))]
    (->> products
         (filter (complement (partial c/attributes-exist? attributes))))))

(defn update-simple-reference-data!
  [where fields]
  (->> (a/f where)
       (filter-existing-reference-data fields)
       (map db/bb-name-of)
       (filter identity)
       vec
       (feed/simple-reference-data (vec fields))
       flatten
       vec
       (partition-all 100)
       (map a/update!)))

(def futures-seeds
  [["KWA Comdty" {:product/type "Future" :product/underlying-product "Wheat (Kansas Wheat)"}]
   ["W A Comdty" {:product/type "Future" :product/underlying-product  "Wheat"}]
   ["LPA Comdty" {:product/type "Future" :product/underlying-product "Copper - Grade A"}]
   ["LXA Comdty" {:product/type "Future" :product/underlying-product "Zinc"}]
   ["LHA Comdty" {:product/type "Future" :product/underlying-product "Lean Hogs"}]
   ["C A Comdty" {:product/type "Future" :product/underlying-product "Corn"}]
   ["KCA Comdty" {:product/type "Future" :product/underlying-product "Coffee \"C\""}]
   ["CTA Comdty" {:product/type "Future" :product/underlying-product "Cotton #2"}]
   ["LCA Comdty" {:product/type "Future" :product/underlying-product "Live Cattle"}]
   ["S A Comdty" {:product/type "Future" :product/underlying-product "Soybeans"}]
   ["XBA Comdty" {:product/type "Future" :product/underlying-product "RBOB Gasoline"}]
   ["HOA Comdty" {:product/type "Future" :product/underlying-product "Heating Oil"}]
   ["LAA Comdty" {:product/type "Future" :product/underlying-product "Aluminium"}]
   ["SIA Comdty" {:product/type "Future" :product/underlying-product "Silver"}]
   ["COA Comdty" {:product/type "Future" :product/underlying-product "Brent Crude"}]
   ["NGA Comdty" {:product/type "Future" :product/underlying-product "Natural Gas"}]
   ["FCA Comdty" {:product/type "Future" :product/underlying-product "Feeder Cattle"}]
   ["ESA Index" {:product/type "Future" :product/underlying-product "SPX Index"}]
   ["CCA Comdty" {:product/type "Future" :product/underlying-product "Cocoa"}]
   ["TPA Index" {:product/type "Future" :product/underlying-product "TPX Index"}]
   ["RTA Index" {:product/type "Future" :product/underlying-product "RTX Index"}]
   ["SBA Comdty" {:product/type "Future" :product/underlying-product "Sugar"}]
   ["CLA Comdty" {:product/type "Future" :product/underlying-product "Light Crude"}]
   ["GCA Comdty" {:product/type "Future" :product/underlying-product "Gold"}]
   ["GXA Index" {:product/type "Future" :product/underlying-product "DAX Index"}]
   ["MFA Index" {:product/type "Future" :product/underlying-product "MDAX Index"}]
   ["TMIA Index" {:product/type "Future" :product/underlying-product "TPX Index"}]])

(defn- default-config
  []
  (let [indices ["SPX Index" "TPX Index" "DAX Index" "MDAX Index"]
        ats-funds #(->> (a/f {:schedule/broker "ATS"})
                        (map :schedule/product)
                        (map db/bb-name-of))]
    [[update-price-data! (ats-funds) feed/historical-bar-data]
     [update-price-data! indices feed/historical-bar-data]
     [update-price-data! ["GBPUSD Curncy" "GBPJPY Curncy"] (partial feed/intraday-slices
                                                                    (t/date-time 2000 1 1 12)
                                                                    (t/time-zone-for-id "Europe/London"))]
     [update-simple-reference-data! :product ["CRNCY" "SECURITY_TYP2"]]
     [update-simple-reference-data! {:product/type "Mutual Fund"} ["FUND_GEO_FOCUS" "FUND_TYP" "FUND_TOTAL_ASSETS" "FUND_TOTAL_ASSETS_CRNCY"]]
     [update-simple-reference-data! {:product/type "Future"} ["LAST_TRADEABLE_DT" "FUT_FIRST_TRADE_DT" "PX_POS_MULT_FACTOR" "FUT_GEN_MONTH"]]
     [update-product-data! (ats-funds) get-dual-or-single-priced]
     [save-futures-chain! futures-seeds]]))

(defn update-data!
  []
  (->> (default-config)
       (map #(apply (first %) (rest %)))))


(comment

  (def xs
    (historical-bar-data
     (t/date-time 2014 7 7)
     (t/date-time 2014 10 31)
     ["SPX Index" "TPX Index"]))

  (->> xs
       (map #(update-in % [:price/time] u/parse-instant))
       (partition-all 50)
       (map a/save!))

  (a/save! (flatten xs) [:price/product :price/time])

  (datomic.api/transact @db/conn [{:db/id :model/product
                                   :db/unique :db.unique/value
                                   ;;:db/index true
                                   :db.alter/_attribute :db.part/db}])

  (def ys
   (->> (a/f :synonym)
        (map :synonym/name)
        distinct
        (map #(a/f {:synonym/name %}))))

  (->> ys
       (filter #(> (count %) 1))
       (map first)
       a/retract-entities!)


  (->> (a/f {:synonym/name "GB00B5TGB445"})
       (map :synonym/product)
       (map :db/id))

  (def y
    (->>  isins-and-tickers
          (map second)
          (partition-all 20)
          (map (partial historical-bar-data
                        (t/date-time 2014 7 7)
                        (t/date-time 2014 10 31)))
          flatten))

  (def y'
   (->> y
        (map (fn [price] (update-in price [:price/product] (comp :db/id db/product-of))))
        (map (fn [price] (update-in price [:price/value] bigdec)))
        (map (fn [price] (update-in price [:price/time] clj-time.coerce/to-date)))))

  (->> y'
       (map #(assoc % :db/id (datomic.api/tempid :db.part/user)))
       (partition-all 100)
       (map (partial datomic.api/transact @db/conn)))

  (count y')

  (simple-reference-data ["PX_LAST"] ["EURJPY Curncy"])

  (simple-reference-data ["FUND_GEO_FOCUS"] ["LU0011963674 Equity"])

  (->> isins
       (map db/product-of)
       (map :db/id))

  (count y)
  (count isins)
  (->> (flatten y)
       (map :product/product)
       distinct
       count)

  (->> y
       flatten
       (map :product/product)
;;       (filter #(= "LU0011963674 Equity" (:product/product %)))
       count)

  ()

  (->> y
       flatten
       (map #(assoc % :db/id (:db/id (db/product-of (:product/product %)))))
       (map #(dissoc % :product/product))
       a/update!)

  (->> isins
       (map db/product-of)
       (filter (complement :product/geo-focus)))

  (a/update! [(ffirst y)])

  (a/update! (flatten y) [:product/name])

  (def y
    (intraday-slices (t/date-time 2014 1 1 14)
                     (t/time-zone-for-id "Europe/London")
                     (t/date-time 2014 7 7)
                     (t/date-time 2014 10 31)
                     ["EURJPY Curncy" "USDJPY Curncy"]))

  (let [syn (-> (a/fu {:synonym/name "GBPJPY Curncy"})
                (dissoc :synonym/product :db/id)
                (assoc :synonym/name "USDJPY Curncy"))
        prod {:product/type "CROSS" :product/currency "JPY"}]
    (db/save-new-products! [syn] [prod]))



  (first y)

  (->> y
       (partition-all 50)
       (map #(a/save! % [:price/product :price/time])))

  (pprint isins)
  (def isins-and-tickers
    (let [lines (-> "./resources/back-office/nomura.csv"
                    slurp
                    (clojure.string/split #"\r\n"))
          lines' (->> lines
                     (map #(clojure.string/split % #","))
                     (map (partial map trim)))
         header (first lines')]
     (->> lines'
          rest
          (map (partial zipmap header))
          (map (juxt #(get % "ISIN")
                     #(get % "TICKER_AND_EXCH_CODE"))))))

  (def new-syns
   (->> isins-and-tickers
        (map (fn [[i t]] {:synonym/name t
                         :synonym/type :synonym.type/ticker
                         :synonym/product (:db/id (db/product-of i))}))))
  (a/save! new-syns [:synonym/name])
  (let [lines' (->> lines
                    (map #(clojure.string/split % #","))
                    (map (partial map trim))
                    )
        header (first lines')]
    header)
  (db/product-of "ALLAMCE LX Equity")

  (->> y;;(a/f {:price/product "GBPJPY Curncy"})
       (sort-by (complement :price/time))
       (take 10))

  (count lines)
  (first lines)
  (def lines
    )
  (def contents (slurp ))
  ()

)
