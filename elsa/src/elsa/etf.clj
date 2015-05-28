(ns schneider.etf
  (:use clojure.pprint
        clojure.set)
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as s]
            ;;[schneider.entity :as e]
            ;;[schneider.product :as p]
            [utilities.core :as u]
            ;;[schneider.schema :as schema]
            ))

(comment
  (def config
    {:daily {:cron-schedule " 0 0 * * 1,2,3,4,5",
             :symbol-list ["DBLCI DIVERSIFIED COMMODITY INDEX BASKET COMMODITY INDEX"
                           "DBLCI DIVERSIFIED AGRICULTURE INDEX BASKET COMMODITY INDEX"
                           "DBIQ OPTIMUM YIELD INDUSTRIAL METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"
                           "DBIQ OPTIMUM YIELD PRECIOUS METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"]}})
  )

(def index-params
  {"DBLCI DIVERSIFIED COMMODITY INDEX BASKET COMMODITY INDEX" {:id "95237" :currency "USD-Local" :category "ER" :rebal "2"}
   "DBLCI DIVERSIFIED AGRICULTURE INDEX BASKET COMMODITY INDEX" {:id "97253" :currency "USD-Local" :category "ER" :rebal "2"}
   "DBIQ OPTIMUM YIELD INDUSTRIAL METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX" {:id "95552" :currency "EUR-Hedged" :category "TR" :rebal "1"}
   "DBIQ OPTIMUM YIELD PRECIOUS METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX" {:id "95551" :currency "EUR-Hedged" :category "TR" :rebal "1"}})

(defn unparse-date [date]
  (f/unparse (f/formatter "MM/dd/yyyy") date))

(defn params [name date]
  (let [ps (get index-params name)]
   {"prevcurrencyreturntype" (:currency ps)
    "returncategory" (:category ps)
    "indexStartDate" "01/15/2000"
    "rebalperiod" (:rebal ps)
    "redirect" "benchmarkIndexConstituent"
    "prevreturncategory" ""
    "indexLaunchDate" "20060531"
    "prevpriceDate" "02/02/2015"
    "prevreportingfrequency" "1"
    "priceDate" (unparse-date date)
    "currencyreturntype" "USD-Local"
    "previndexStartDate" "01/15/2000"
    "indexInceptionDate" "19970903"
    "prevIndexStartDate" "01/15/2000"
    "pricegroup" "FIX"
    "indexid" (:id ps)}))

(def url "https://index.db.com/dbiqweb2/servlet/indexsummary")

(defn- get-index-html
  [name date]
  (->> (client/get
        url
        {:query-params (params name date)})
       :body))

(defn extract-holdings
  [html-]
  (let [rows (-> html-
                 html/html-snippet
                 (html/select [[:table (html/attr= :title "Basket Commodity Index")] :tr]))
        header ["Price Date" "Name" "Expiry Date" "Weight" "Base Weight"]]
    (->> rows
         rest
         (map #(html/select % [:td]))
         (map (partial map (comp first :content)))
         (map (partial zipmap header)))))

(defn parse-holding
  [index-name holding]
  (let [kmap {"Price Date" :holding/time
              "Weight" :holding/weight}
        product-kmap {"Name" :product/underlying-product
                      "Expiry Date" :underlying/expiry-date}
        product (-> holding
                    (select-keys (keys product-kmap))
                    (rename-keys product-kmap))]
    (-> holding
        (select-keys (keys kmap))
        (rename-keys kmap)
        (assoc :holding/owner index-name)
        (assoc :holding/product product))))

(defn get-holdings
  [name date]
  (->> (get-index-html name date)
       extract-holdings
       (map (partial parse-holding name))))

(comment

  (get-holdings "DBIQ OPTIMUM YIELD INDUSTRIAL METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"
                (t/date-time 2014 1 14))


  )

(defn powershares-url
  [fund]
  (str "https://www.invesco.com/portal/site/us/financial-professional/etfs/product-detail?productId=" fund))

(defn powershares-positions
  [fund]
  (let [rows (-> fund
                 powershares-url
                 html-resource
                 (html/select [[:table.products-table (html/has [(html/re-pred (re-pattern "Commodity"))])] :tbody :tr]))
        header [:underlying :expiry :holding/weight]]
    (->> rows
         (map #(html/select % [:td]))
         (map (partial map (comp clojure.string/trim first :content)))
         (map (partial zipmap header))
         (map #(assoc % :holding/owner fund)))))

(comment

  (count rows-)
  (def xs (powershares-positions "DBC"))




         (map #(assoc % :holding/product (p/lookup-unique-future {:product/underlying (:underlying %)
                                                                  :product/expiry (f/parse (f/formatter "dd-MMM-yyyy") (:expiry %))})))

  (def xs (dbiq-positions (t/date-time 2014 6 6) "DBLCI DIVERSIFIED COMMODITY INDEX BASKET COMMODITY INDEX"))

  (->> xs
       first
       :underlying
       )



  ((e/find {:synonym/name "Aluminium"}))

)
