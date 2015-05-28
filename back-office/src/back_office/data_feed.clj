(ns back-office.data-feed
  (:use clojure.pprint
        utilities.core
        clojure.set)
  (:require [clj-http.client :as client]
            [ring.middleware.params :as p]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as format]
            [clj-time.coerce :as ce]
            [db.core :as db]))

(defn- api-url [] "http://10.22.12.43:3000/api")

(defn- unparse-param
  [[k v]]
  (let [v' (if (string? v) (str "\"" v "\"") (str v))]
   [k v']))

(defn- unparse-params
  [{params :query-params}]
  {:query-params
   (->> params
        (into [])
        (map unparse-param)
        (into {}))})

(defn- call-api
  [params]
  (->> params
       (assoc {} :query-params)
       unparse-params
       (client/get (api-url))
       :body
       read-string))

(defn remove-nil-values
  [entity']
  (->> entity'
       (into [])
       (filter (fn [[k v]] v))
       (into {})))

(defn- partition-and-call-api
  [prefix partial-params securities]
  (let [params (->> securities
                    (partition-all 50)
                    (map vec)
                    (map #(assoc partial-params :securities %)))
        responses (doall
                   (for [p params]
                     (do
                      (Thread/sleep 500)
                      (call-api p))))]
    (->> responses
         flatten
         (map remove-nil-values)
         (filter #(->> % (into []) count (< 1)))
         (map #(rename-keys % (db/bloomberg-attribute-map prefix))))))

(defn simple-reference-data
  [fields securities]
  (partition-and-call-api
   :product
   {:fields fields
    :api-function 'simple-reference-data}
   securities))

(defn bulk-reference-data
  [fields sub-fields securities]
  (partition-and-call-api
   :product
   {:fields fields
    :sub-fields sub-fields
    :api-function 'bulk-reference-data}
   securities))

(defn- assoc-price-fields
  [price]
  (-> price
      (assoc :price/bat :price.bat/trade)
      (assoc :price/aspect :price.aspect/close)
      (assoc :price/source :price.source/bloomberg)))

(defn historical-bar-data
  [start end securities]
  (->> securities
       (partition-and-call-api
        :price
        {:start (format-bb-date start)
         :end (format-bb-date end)
         :api-function 'historical-bar-data})
       (map assoc-price-fields)
       (map #(update-in % [:price/time] parse-instant))))

(defn- format-bb-time
  [time]
  (format/unparse (format/formatters :date-hour-minute-second) time))

(defn intraday-bar-data
  [start end interval securities]
  (->> securities
       (partition-and-call-api :price
                               {:start (format-bb-time start)
                                :end (format-bb-time end)
                                :interval interval
                                :api-function 'intraday-bar-data})
       (map #(update-in % [:price/time] (fn [dt] (t/plus (parse-instant dt) (t/minutes interval)))))
       (map assoc-price-fields)))

(defn intraday-bars
  [securities end]
  (let [interval 60
        start (t/minus end (t/minutes interval))]
    (intraday-bar-data start end interval securities)))

(defn intraday-slices
  [time timezone start end securities]
  (->> (working-days start end)
       (map #(merge-date-and-time % timezone time))
       (map (partial intraday-bars securities))
       flatten))

(comment





  (intraday-bars ["GBPUSD Curncy"] (t/date-time 2014 6 6 13))
  (simple-reference-data ["CRNCY" "SECURITY_TYP2"] ["TPX Index"])
  (simple-reference-data ["LAST_PRICE"] ["TPX Index"])
  (bulk-reference-data ["FUT_CHAIN"] [["Security Description"]] ["TPX Index"])
  (historical-bar-data (t/date-time 2014 6 6) (t/date-time 2014 6 6) ["TPX Index"])

  (intraday-bar-data (t/date-time 2014 6 4 12) (t/date-time 2014 6 6 13) 60 ["GBPUSD Curncy"])

  (intraday-slices (t/date-time 2000 1 1 3) (t/time-zone-for-id "Europe/London") (t/date-time 2014 6 1) (t/date-time 2014 6 30) ["TPX Index"])

  )


(comment

  (get-intraday-slices
   (t/date-time 2000 1 1 12)
   (t/time-zone-for-id "Europe/London")
   (t/date-time 2014 6 1)
   (t/date-time 2014 6 30)
   ["GBPUSD Curncy"])

  (def chain (db/select-where :product {:product/underlying "MDAX Index"}))

  (def prices (get-intraday-bars (t/date-time 2014 1 1) (t/now) "MFM4 Index" 1))

  (db/save! :price prices)

  (count prices)

  (get-historical-data (t/date-time 2014 6 1) (t/date-time 2014 6 30) ["SPX Index"])

  (f-get-reference-data ["LAST_PRICE"] ["SPX Index"])

  (get-bulk-data ["INDX_MWEIGHT"] ["Member Ticker and Exchange Code" "Percentage Weight"] ["UKX Index"])

  (get-historical-data (t/date-time 2014 6 29) (t/date-time 2014 6 30) ["SPX Index"])
  (->> ["SDAX" "MDAX" "DAX" "OSESX" "OBX" "CS90" "CM100" "CAC" "SMX"	"MCX" "MCIX" "UKX" "TPXSM" "TPXM400" "TPX" "RTY" "RIY"
        "SITA" "MITA" "FTSEMIB" "MXITSC" "MXITMC" "MXITLC" "SSPXX" "MSP" "LSP" "IBEXS" "IBEXM" "IBEX" "SIRE"	"MIREXX" "LIRE"
        "SFINX" "MFINX" "LFIN" "SNETH" "MNETH" "LNETH" "MXCHSC" "MXCHMC" "MXCHLC" "SSWED" "MSWED" "LSWED" "SDEN" "MDEN" "LDEN"]
       (map (fn [s] (str s " Index")))
       (map vector)
       (map (partial get-historical-data! (t/date-time 2000 1 1) (t/date-time 2014 1 1)))
       )

  (db/select-where :product {:product/name "SDAX Index"})
  (->> (db/select-where :price {:price/product "SDAX Index"})
       (sort-by :price/time)
       first)

  (get-historical-data! (t/date-time 2000 1 1) (t/date-time 2014 1 1) ["MDAX Index" "OSESX"])

  (get-reference-data- ["TICKER"] ["TPX Index" "SPX Index"])

  (get-bulk-data ["INDX_MWEIGHT"] [["Member Ticker and Exchange Code" "Percentage Weight"]] ["TPX Index"])

  (get-reference-data ["TICKER"] ["TPX Index" "SPX Index"])

  (get-bulk-data ["FUT_CHAIN"] [["Security Description"]] ["TPX Index"])

  )
