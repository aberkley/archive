(ns back-office.test.handler
  (:require [clojure.test :refer :all]
            [datomic.api :only [q db] :as d]
            [entomic.core :as e]
            [entomic.api :as a]
            [entomic.format :as ft]
            [entomic.coerce]
            [db.schema :as s]
            [db.core :only [save-new-products!] :as dbc]
            [back-office.statements :as st]
            [back-office.transaction :as tr]
            [clj-time.core :as t])
  (:import [java.io File]))

(e/resolve-api! (find-ns 'datomic.api))

(def uri "datomic:mem://test")

(d/delete-database uri)

(d/create-database uri)

(e/set-connection! uri)

(dbc/initialise!
   (find-ns 'entomic.core)
   (find-ns 'entomic.format)
   (find-ns 'entomic.api)
   (find-ns 'entomic.coerce))

(e/transact e/conn s/schema)

(def files
  {:fund-confirms (File. "./resources/test/confirms-2014-10-02.htm")
   :fund-orders   (File. "./resources/test/orders-2014-10-02.htm")
   :futures-confirms (File. "./resources/test/20140903-1491-A237_TRAD_2_1-TRX (L)-9007594.xml")
   :futures-fills (File. "./resources/test/cqg-2014-09-02.csv")
   :fund-contract-notes (->> ["./resources/test/Document_20141013132524.pdf"
                              "./resources/test/Document_20141013132522.pdf"
                              "./resources/test/Document_20141013132530.pdf"
                              "./resources/test/Document_20141013132528.pdf"
                              "./resources/test/Document_20141013132525.pdf"
                              "./resources/test/Document_20141013132526.pdf"]
                             (map #(File. %)))
   :abn-positions (File. "./resources/test/20140912-1491-A237_TRAD_2_1-POS (L)-9281791.xml")})

(defn extract-product-names
  [files]
  (->> files
       (map st/statements)
       flatten
       (map :statement/product)
       (filter string?)
       distinct))

(defn save-product-names!
  [names]
  (let [syns (map (fn [name] {:synonym/name name}) names)
        prods (repeat (count syns) {:product/type "Test"})]
    (dbc/save-new-products! syns prods)))

(defn save-test-products! []
  (-> files (dissoc :futures-fills) vals flatten extract-product-names save-product-names!)
  (save-product-names! (flatten ["TPIXF" "GBP/JPY" "USD.EUR" "JPY.EUR" "GBP.EUR" tr/interests]))
  (dbc/save-new-products!
   [{:synonym/name "JTPXZ14"}]
   [{:product/underlying-product (:db/id (dbc/product-of "TPIXF"))
     :product/expiry (clj-time.coerce/to-date
                      (t/date-time 2014 12 15))
     :product/multiplier 10000M
     :product/currency "JPY"}]))

(deftest test-match-funds
  (is (boolean (save-test-products!)))
  (is (boolean (st/save-statements! (:fund-orders files))))
  (is (boolean (tr/save-open-fund-transactions!)))
  (is (= 9 (count (a/f {:transaction/status "Open"}))))
  (is (boolean (st/save-statements! (:fund-confirms files))))
  (is (boolean (tr/match-fund-transactions-in-db!)))
  (is (= 7 (count (a/f {:transaction/status "Partially Confirmed"}))))
  (is (= 2 (count (a/f {:transaction/status "Open"}))))
  (is (every? identity (doall (map st/save-statements! (:fund-contract-notes files)))))
  (is (boolean (tr/match-partial-fund-statements-in-db!)))
  (is (= 4 (count (a/f {:transaction/status "Confirmed"}))))
  (is (boolean (st/save-statements! (:abn-positions files))))
  (is (boolean (st/save-statements! (:futures-fills files))))
  (is (boolean (st/save-statements! (:futures-confirms files))))
  (is (= 3 (count (a/f {:statement/broker "ABN" :statement/product "JTPXZ14" :statement/status "Filled" :statement/matched-status "Unmatched"}))))
  (is (= 3 (count (a/f {:statement/broker "ABN" :statement/product "JTPXZ14" :statement/status "Confirmed" :statement/matched-status "Unmatched"}))))
  (is (boolean (tr/save-fills!)))
  (is (= 3 (count (a/f {:transaction/broker "ABN" :transaction/product "JTPXZ14" :transaction/status "Filled"}))))
  (is (boolean (tr/match-abn-transactions-in-db!)))
  (is (= 3 (count (a/f {:transaction/broker "ABN" :transaction/product "JTPXZ14" :transaction/status "Confirmed"})))))

(comment
  (e/find- query)
  (ft/parse-ref query)
  (ft/resolver :db.type/ref :position/product query)
  (ft/resolve-value :db.type/ref :position/product query)
  (a/id query)
  (->> files
       :abn-positions
       st/statements
       ;;(filter :price/time)
       ;;first
       ;;:position/product
       ;;a/fu
       ;;ft/resolve-
       a/save!
       )
  )
