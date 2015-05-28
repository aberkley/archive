(ns back-office.transaction
  (:use     clojure.pprint
            clojure.set)
  (:require [entomic.api :as a]
            [entomic.coerce :as c]
            [db.core :as db]
            [back-office.schedule :as schedule]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [utilities.core :as u]))

(defn abn-usd-rate
  [currency time]
  (let [numerator (a/fu {:price/time time :price/product (str currency ".EUR") :price/source :price.source/abn})
        denominator (a/fu {:price/time time :price/product "USD.EUR" :price/source :price.source/abn})]
    (if (and numerator denominator)
      (/ (float (:price/value numerator))
         (float (:price/value denominator))))))

(defn units-match?
  [transaction statement]
  (u/bigdec= (:transaction/units transaction) (:statement/units statement)))

(defn value-match?
  [transaction statement]
  (if (:transaction/value transaction)
    (u/bigdec= (:statement/value statement)
             (:transaction/value transaction))))

(defn name-match?
  [transaction statement]
  (= (:transaction/product transaction) (:statement/product statement)))

(defn jpy-trade-date-match?
  "Japanese futures traded in the after-hours session are booked the next day"
  ;; TODO: move back only 1 working day but know about holidays
  [transaction statement]
  (let[t-date (:transaction/trade-date transaction)
       s-date (:statement/trade-date statement)]
    (and
     (= (:statement/currency statement) (:transaction/currency transaction) "JPY")
     (or (= (u/date-dec s-date) t-date)
         (= (u/date-dec s-date 2) t-date)))))

(defn trade-date-match?
  [transaction statement]
  (or (= (:transaction/trade-date transaction) (:statement/trade-date statement))
      (jpy-trade-date-match? transaction statement)))

(defn name-and-value-match?
  [transaction statement]
  (let [v? (value-match? transaction statement)
        u? (units-match? transaction statement)
        n? (name-match? transaction statement)]
    (and n? (or v? u?))))

(defn contract-note-match?
  [transaction statement]
  (and (name-match? transaction statement)
       (units-match? transaction statement)
       (trade-date-match? transaction statement)))

(defn approximate-price-match?
  [transaction statement]
  (=
   (format ".4f" (:transaction/price transaction))
   (format ".4f" (:statement/price statement))))

(defn price-match?
  [transaction statement]
  (or
   (u/bigdec= (:transaction/price transaction) (:statement/price statement))
   (approximate-price-match? transaction statement)))

(defn full-match?
  [transaction statement]
  (and
   (name-match? transaction statement)
   (units-match? transaction statement)
   (price-match? transaction statement)
   (trade-date-match? transaction statement)))

(defn match
  [match? {transaction :transaction :as out} statement]
  (if (and (not (nil? transaction)) (match? transaction statement))
    (-> out
        (assoc :matched [transaction statement])
        (dissoc :transaction))
    (-> out
        (update-in [:unmatched-statements] conj statement))))

(defn match-transaction
  [match? {statements :unmatched-statements :as out} transaction]
  (let [m (reduce (partial match match?) {:transaction transaction :unmatched-statements [] :matched nil} statements)]
    (-> out
        (update-in [:unmatched-transactions] #(if-let [o (:transaction m)] (conj % o) %))
        (assoc :unmatched-statements (:unmatched-statements m))
        (update-in [:matched] #(if-let [m- (:matched m)] (conj % m-) %) ))))

(defn match-transactions
  [match? transactions statements]
  (reduce
   (partial match-transaction match?)
   {:unmatched-transactions [] :unmatched-statements statements :matched []}
   transactions))

(defn group-by-product-date-currency
  [entities ks]
  (let [a   (c/entity-prefix (first entities))
        u   (c/attribute-of a :units)
        ks' (->> ks
                 (map (partial c/attribute-of a)))
        gs  (group-by (apply juxt ks') entities)]
    [gs
     (->> gs
          (map (fn [[p' ys]] [p' (->> ys
                                     (map u)
                                     (reduce +))]))
          (into {}))]))

(defn match-fees
  [transactions statements]
  (let [transactions'      (->> transactions (map #(update-in % [:transaction/trade-date] (fn [d] (u/date-inc d 2)))))
        [t-group t-summed] (group-by-product-date-currency transactions')
        [s-group s-summed] (group-by-product-date-currency statements)
        ks                 (intersection (set (keys t-group))
                                         (set (keys s-group)))
        ks'                (->> ks
                                (filter (fn [k] (u/bigdec= (t-summed k)
                                                          (s-summed k)))))]
    (->> {:matched-statements     (select-keys s-group ks')
         :matched-transactions   (select-keys t-group ks')
         :unmatched-statements   (apply dissoc s-group ks')
         :unmatched-transactions (apply dissoc t-group ks')}
         (map (fn [[k v]] [k (flatten (vals v))]))
         (into {}))))

(defn update-matched-fees!
  [{ts :matched-transactions ss :matched-statements}]
  (let [ts' (->> ts
                 (map #(assoc % :transaction/status "Confirmed")))
        ss' (->> ss
                 (map #(assoc % :statement/matched-status "Matched")))]
    (a/as-transaction!
     [:update ts' (:transaction db/default-keys)]
     [:update ss' (:statement db/default-keys)])))

(defn match-fees-from-db!
  []
  (let [transactions (->> (a/f {:transaction/status "Open"})
                          (filter #(-> %
                                       :transaction/product
                                       db/product-type-of
                                       (= "Fee"))))
        statements (->> (a/f {:statement/matched-status "Unmatched"})
                        (filter #(-> %
                                     :statement/product
                                     db/product-type-of
                                     (= "Fee"))))]
    (update-matched-fees!
     (match-fees transactions
                 statements))))

(defn statement-as-transaction
  [statement]
  (-> statement
      (dissoc :statement/matched-status :statement/file-name :db/id)
      (c/update-attribute-prefixes :transaction)))

(defn merge-statement-and-transaction
  [[transaction statement]]
  (let [s (statement-as-transaction statement)
        t (-> transaction
              (dissoc :transaction/status))]
    (merge s t)))

(defn update-matched!
  [{matched :matched}]
  (let [transactions (->> matched
                          (map merge-statement-and-transaction)
                          (map (fn [t] (dissoc t :transaction/matched-status))))
        statements (->> matched
                        (map (fn [[o c]] (assoc c :statement/matched-status "Matched"))))]
    (if (seq matched)
     (a/as-transaction!
      [:update transactions (:transaction db/default-keys)]
      [:update statements (:statement db/default-keys)]))))

(defn match-fund-transactions-in-db! []
  (let [transactions   (a/f {:transaction/broker "ATS" :transaction/status "Open"})
        statements (a/f {:statement/broker "ATS" :statement/status "Partially Confirmed" :statement/matched-status "Unmatched"})]
    (-> (match-transactions name-and-value-match? transactions statements)
        update-matched!)))

(defn match-partial-fund-statements-in-db! []
  (let [partials   (a/f {:transaction/broker "ATS" :transaction/status "Partially Confirmed"})
        statements (a/f {:statement/broker "ATS" :statement/status "Confirmed" :statement/matched-status "Unmatched"})]
    (-> (match-transactions contract-note-match? partials statements)
        update-matched!)))

(defn match-abn-transactions-in-db! []
  (let [transactions (a/f {:transaction/broker "ABN" :transaction/status "Filled"})
        statements (a/f {:statement/broker "ABN" :statement/status "Confirmed" :statement/matched-status "Unmatched"})]
    (-> (match-transactions full-match? transactions statements)
        update-matched!)))

(defn save-as-transactions!
  [statements]
  (let [transactions (->> statements
                          (map statement-as-transaction))
        statements' (->> statements
                         (map #(assoc % :statement/matched-status "Matched")))]
    (if (seq statements)
      (a/as-transaction!
       [:update statements' (:statement db/default-keys)
        :save transactions (:transaction db/default-keys)]))))

(defn save-open-fund-transactions! []
  (let [ss (a/f {:statement/broker "ATS" :statement/status "Open" :statement/matched-status "Unmatched"})
        ts (map statement-as-transaction ss)
        ss' (map #(assoc % :statement/matched-status "Matched") ss)]
    (if (seq ss)
     (a/as-transaction!
      [:update ss' (:statement db/default-keys)]
      [:save ts (:transaction db/default-keys)]))))

(defn save-fills! []
  (let [statements (a/f {:statement/broker "ABN" :statement/status "Filled" :statement/matched-status "Unmatched"})
        transactions (->> statements
                          (map statement-as-transaction))
        statements' (->> statements
                         (map #(assoc % :statement/matched-status "Matched")))
        fees (->> transactions
                  (map schedule/fees-of))
        tfs (->> [statements' transactions fees]
                 (apply map (fn [s t f] [s t f]))
                 (filter (fn [[s t f]] (every? identity f))))]
    (if (seq statements)
     (a/as-transaction!
      [:save (flatten (map rest tfs)) (:transaction db/default-keys)]
      [:update (map first tfs) (:statement db/default-keys)]))))

;; until fees are working!
(defn save-fills! []
  (let [statements (a/f {:statement/broker "ABN" :statement/status "Filled" :statement/matched-status "Unmatched"})
        transactions (->> statements
                          (map statement-as-transaction))
        statements' (->> statements
                         (map #(assoc % :statement/matched-status "Matched")))]
    (if (seq statements)
      (a/as-transaction!
       [:save transactions (:transaction db/default-keys)]
       [:update statements' (:statement db/default-keys)]))))

(def fees
  ["SUB-CLEARER/BROKER FEE TRADE Futures"
   "EUREX TRADING FEE Futures"
   "US TRADING FEE Futures"
   "CLEARING FEE TRADE Others"
   "US CCP FEE Futures"
   "CLEARING FEE TRADE Futures"
   "NFA FEE Futures"])

(def interests
  ["ACCRUED INTEREST CASH"
   "ACCRUED INTEREST MARGIN"
   "INTEREST EXPENSE CASH"
   "INTEREST EXPENSE MARGIN"])

(defn save-interests! []
  (->> interests
       (map (fn [p] {:statement/broker "ABN" :statement/status "Confirmed" :statement/matched-status "Unmatched" :statement/product p}))
       (map a/f)
       flatten
       save-as-transactions!))

(defn process-all-transactions!
  []
  (save-open-fund-transactions!)
  (save-fills!)
  (save-interests!)
  (match-fund-transactions-in-db!)
  (match-partial-fund-statements-in-db!)
  (match-abn-transactions-in-db!))
