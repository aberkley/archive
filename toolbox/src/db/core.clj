(ns db.core
  (:require [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [utilities.ns :as n]
            [db.schema :as s]))

;; bin\transactor config\samples\free-transactor-template.properties

(def transact nil)

(declare connect)

(declare set-custom-parser!)

(declare f)

(declare fu)

(declare attribute-of)

(declare tempid)

(declare conn)

(declare product-of)

(declare uri)

(defonce db-uri "datomic:free://localhost:4334/schneider")

(def product-idents
  [:schedule/product,
   :holding/owner,
   :holding/product,
   :position/product,
   :model/product,
   :model/benchmark,
   :transaction/product,
   :statement/product,
   :product/underlying-product,
   :price/product])

(defn initialise!
  [core format api coerce]
  (let [ns (find-ns 'db.core)]
    (n/intern-ns core ns)
    (n/intern-ns format ns)
    (n/intern-ns api ns)
    (n/intern-ns coerce ns)
    (transact (@connect @uri) s/schema)
    (defn product-of
      [x]
      (let [t (type x)]
        (cond
         (= t java.lang.Number)
         (fu {:db/id x})
         (= t java.lang.String)
         (:synonym/product
          (fu {:synonym/name x}))
         :else x)))
    (set-custom-parser! product-idents product-of)))

(def default-keys
  {:price       [:price/product :price/time :price/bat :price/aspect]
   :schedule    [:schedule/product :schedule/broker]
   :model       [:model/product]
   :transaction [:transaction/product :transaction/order-time :transaction/units :transaction/price]
   :statement   [:statement/product :statement/order-time :statement/fill-time :statement/units
                 :statement/value :statement/status :statement/internal-reference :statement/external-reference :statement/trade-date]
   :synonym     [:synonym/name]
   :product     []
   :holding     [:holding/time :holding/owner :holding/product]
   :position    [:position/time :position/product :position/currency :position/settlement-date :position/units]})

(def primary-name-keys
  (reverse
   [:synonym.type/isin
     :synonym.type/bb-isin
     :synonym.type/ticker
     :synonym.type/ats-name
     :synonym.type/ats-symbol
     :synonym.type/abn-name
     :synonym.type/cqg-symbol
    :synonym.type/abn-name-2
    nil]))

(def bb-name-keys [:synonym.type/ticker :synonym.type/bb-isin])

(def display-name-keys (reverse primary-name-keys))

(defn- bloomberg-attribute-suffix-map
  []
  {"date"               :time
   "LAST_PRICE"         :value
   "security"           :product
   "time"               :time
   "close"              :value
   "CRNCY"              :currency
   "SECURITY_TYP2"      :type
   "FUND_GEO_FOCUS"     :geo-focus
   "FUND_TYP"           :fund-type
   "LAST_TRADEABLE_DT"  :expiry
   "FUT_FIRST_TRADE_DT" :first-trade
   "PX_POS_MULT_FACTOR" :multiplier
   "FUT_GEN_MONTH"      :fut-gen-month
   "FUND_TOTAL_ASSETS"  :fund-total-assets
   "FUND_TOTAL_ASSETS_CRNCY" :fund-total-assets-currency})

(defn bloomberg-attribute-map
  [prefix]
  (->> (bloomberg-attribute-suffix-map)
       (into [])
       (map (fn [[k v]] [k (attribute-of prefix v)]))
       (into {})))

(comment
  (defn synonym
    [name type product]
    {:synonym/name name
     :synonym/type type
     :db/id (d/tempid :db.part/user)
     :synonym/product (:db/id product)})

  (defn transaction-synonym-group
    [database synonyms product]
    (let [p-id (d/tempid :db.part/user)]
      (conj
       (for [s synonyms]
         (-> s
             (assoc :synonym/product p-id)
             (assoc :db/id (d/tempid :db.part/user))))
       (assoc product :db/id p-id)))))

(defn product-type-of
  [x]
  (-> x
      product-of
      :product/type))

(defn name-of
  [name-keys product]
  (let [synonyms (f {:synonym/product product})
        name-map (zipmap name-keys (range (count name-keys)))]
    (->> synonyms
         (filter #(contains? name-map (:synonym/type %)))
         (sort-by #(get name-map (:synonym/type %)))
         last
         :synonym/name)))

(def untyped-name-of (partial name-of [nil]))

(def primary-name-of (partial name-of primary-name-keys))

(def display-name-of (partial name-of display-name-keys))

(def bb-name-of (partial name-of bb-name-keys))

(defn date-range
  [start end]
  `[(~'>= ~(tc/to-date start))
    (~'<= ~(tc/to-date end))])

(defn expiry-month
  ([month year]
     (date-range (t/first-day-of-the-month year month)
                 (t/last-day-of-the-month year month)))
  ([dt]
     (expiry-month (t/month dt)
                 (t/year dt))))

(defn save-new-products!
  [synonyms products]
  {:pre (= (count synonyms) (count products))}
  (let [p-ids (for [p products] (tempid :db.part/user))
        s-ids (for [s synonyms] (tempid :db.part/user))
        entities (map (fn [s p s-id p-id]
                        (if-not (fu {:synonym/name (:synonym/name s)})
                          [(assoc p :db/id p-id)
                           (-> s
                               (assoc :db/id s-id)
                               (assoc :synonym/product p-id))]))
                      synonyms
                      products
                      s-ids
                      p-ids)
        entities' (if entities (clojure.core/filter identity entities))]
    (if (seq entities') (@transact @conn (flatten entities')))))
