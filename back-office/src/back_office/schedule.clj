(ns back-office.schedule
  (:use back-office.crossrate
        clojure.math.numeric-tower)
  (:require [entomic.api :as a]
            [clj-time.core :as t]))

(defn round-to
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn per-value
  [prop]
  (fn [{v :transaction/value}] (* prop (abs v))))

(defn per-lot
  [amount]
  (fn [{u :transaction/units}] (* (abs u) amount)))

(defn per-clip
  [amount]
  (fn [t] amount))

(defn per-dollar-value
  [prop]
  (fn [{c :transaction/currency v :transaction/value d :transaction/trade-date}]
    (let [fx (abn-fx-rate c "USD" d)]
      (if fx (round-to 2 (* prop (abs v) fx))))))

(defn fixed-currency
  [c]
  (fn [t] c))

(defn transaction-currency
  [{c :transaction/currency}] c)

(def fx
  {:product/type "CROSS"})

(def any-topix
  {:product/underlying-product "TPX Index"
   :product/currency "JPY"})

(def large-topix
  (assoc any-topix '(int :product/multiplier) 10000))

(def mini-topix
  (assoc any-topix '(int :product/multiplier) 1000))

(def mini-s&p
  {:product/underlying-product "SPX Index"
   :product/currency "USD"
   '(int :product/multiplier) 50})

(def dax
  {:product/underlying-product "DAX Index"})

(def mdax
  {:product/underlying-product "MDAX Index"})

(def all-schedules
  [;; TOPIX
   [large-topix    [[(t/date-time 2014 1 1)  [["SUB-CLEARER/BROKER   FEE TRADE Futures" (per-clip 161M) transaction-currency]
                                              ["CLEARING FEE TRADE Futures"             (per-lot 0.5M)  (fixed-currency "GBP")]]]
                    [(t/date-time 2014 4 15) [["SUB-CLEARER/BROKER   FEE TRADE Futures" (per-clip 160M) transaction-currency]
                                              ["CLEARING FEE TRADE Futures"             (per-lot 0.3M)  (fixed-currency "EUR")]
                                              ["CLEARING FEE TRADE Futures"             (per-lot 130M)  transaction-currency]]]]]
   [mini-topix     [[(t/date-time 2014 1 1)  [["SUB-CLEARER/BROKER   FEE TRADE Futures" (per-lot 9M)    transaction-currency]
                                              ["CLEARING FEE TRADE Futures"             (per-lot 0.5M)  (fixed-currency "GBP")]]]
                    [(t/date-time 2014 4 15) [["CLEARING FEE TRADE Futures"             (per-lot 0.3M)  (fixed-currency "EUR")]
                                              ["CLEARING FEE TRADE Futures"             (per-lot 130M)  transaction-currency]]]]]
   ;; S&P
   [mini-s&p       [[(t/date-time 2014 1 1)  [["CLEARING FEE TRADE Futures"             (per-lot 0.25M) transaction-currency]
                                              ["US TRADING FEE Futures"                 (per-lot 0.75M) transaction-currency]
                                              ["NFA FEE Futures"                        (per-lot 0.02M) transaction-currency]
                                              ["US CCP FEE Futures"                     (per-lot 0.40M) transaction-currency]]]
                    [(t/date-time 2014 7 1)  [["CLEARING FEE TRADE Futures"             (per-lot 0.15M) transaction-currency]
                                              ["US TRADING FEE Futures"                 (per-lot 0.75M) transaction-currency]
                                              ["NFA FEE Futures"                        (per-lot 0.02M) transaction-currency]
                                              ["US CCP FEE Futures"                     (per-lot 0.40M) transaction-currency]]]]]
   ;; DAX
   [dax            [[(t/date-time 2014 1 1)  [["CLEARING FEE TRADE Futures"             (per-lot 0.25M) transaction-currency]]]]]
   [mdax           [[(t/date-time 2014 1 1)  [["CLEARING FEE TRADE Futures"             (per-lot 0.25M) transaction-currency]]]]]
   ;; FX
   [fx             [[(t/date-time 2014 1 1)  [["CLEARING FEE TRADE Others" (per-dollar-value 20/1000000) (fixed-currency "USD")]]]
                    [(t/date-time 2014 7 1)  [["CLEARING FEE TRADE Others" (per-dollar-value 10/1000000) (fixed-currency "USD")]]]]]])

(defn schedule-at
  [{td :transaction/trade-date} schedules]
  (->> schedules
       (filter (fn [[d fees]] (t/after? td d)))
       first
       second))

(defn product-match?
  [spec {product :transaction/product}]
  (let [p-ids (->> spec a/f (map :db/id) (into #{}))]
    (contains? p-ids (:db/id product))))

(defn fee-of
  [transaction [spec product-schedules]]
  (if (product-match? spec transaction)
    (let [schedule (schedule-at transaction product-schedules)]
      (->> schedule
           (map (fn [[p u c]]
                  (if-let [u' (u transaction)]
                    (-> transaction
                        (assoc :transaction/product p)
                        (assoc :transaction/units u')
                        (assoc :transaction/currency (c transaction))
                        (assoc :transaction/price 1M)
                        (assoc :transaction/value u')
                        (assoc :transaction/primary-transaction (:db/id transaction))
                        (assoc :transaction/status "Open")
                        (dissoc :db/id)))))))))

(defn fees-of
  [transaction]
  (try
   (->> all-schedules
        (map (partial fee-of transaction))
        (filter identity)
        flatten)
   (catch Exception e nil)))
