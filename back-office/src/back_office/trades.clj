(ns schneider.trades
  (:require [schneider.position :as pos]
            [schneider.entity :as entity]
            [clj-time.core :as t]
            [clj-time.format :as f]))

;; TODO: calculate possible buys / sells here

(defn trade-date-offset
  [transaction]
  (let [order-date (f/unparse (f/formatters :date)
                              (f/parse (f/formatters :mysql)
                                       (:transaction/order-time transaction)))
        t+ (t/in-days
            (t/interval (f/parse (f/formatters :date) "2014-05-12")
                        (f/parse (f/formatters :date) "2014-05-13")))]
    {:product (:transaction/product transaction)
     :trade-date-offset t+
     :order-date order-date}))

(comment
  (trade-date-offset (first (db/select-where :transaction {:transaction/broker "ATS"})))




  ;; last trade date
  ;; t+ offsets
  ;; rsq
  ;; beta
  ;; name
  ;; ATS symbol
  (first (trade-date-offsets))
  )

(defn trade-date-offsets
  []
  (->> (db/select :transaction)
       (map trade-date-offset)
       (group-by :transaction/product)))

(defn blah
  []
  (let [models (group-by :model/benchmark (db/select-where :model {:model/approved? true}))]
    models))

(comment
  (possible-japan-buys)

(defn possible-japan-buys
  []
  (-> (possible-buys)
      (get "TPX Index")
      ))

(defn possible-sells
  [time]
  ((-> time
       pos/settled-fund-positions
       (dissoc :status))))

  )
