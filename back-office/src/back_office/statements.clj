(ns back-office.statements
  (:use     [clojure.pprint]
            [utilities.core]
            [clojure.math.numeric-tower])
  (:require [net.cgrand.enlive-html :as html]
            [net.cgrand.xml :as xml]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [entomic.api :as a]
            [entomic.coerce :as ec]
            [db.core :as db])
  (:import  [java.io File OutputStreamWriter FileOutputStream BufferedWriter]
            [org.apache.pdfbox.pdmodel PDDocument]
            [org.apache.pdfbox.util PDFTextStripper]
            [java.net URL]))

(defn value-in-£
  [x]
  (-> x
      (s/split #"£")
      second
      (s/replace "," "")
      bigdec))

(defn quantity
  [x]
  (-> x
      (s/replace "," "")
      bigdec))

(defn text-of-pdf
  [file]
  (with-open [pd (PDDocument/load file)]
    (let [stripper (PDFTextStripper.)]
      (.getText stripper pd))))

(defn find-line-number
  [line-text lines]
  (->> lines
       (zipmap (range (count lines)))
       (into [])
       (reduce (fn [x [n t]] (or x (if (= t line-text) n))) nil)))

(defn extract-values
  [file headers]
  (let [lines (->> file
                   text-of-pdf
                   s/split-lines)
        line-text (reduce (fn [a b] (str a " " b)) headers)
        n (find-line-number line-text lines)
        values (s/split (nth lines (+ n 1)) #" ")]
    (zipmap headers values)))

(defn extract-security-name-and-buy-or-sell
  [file]
  (let [lines (->> file text-of-pdf s/split-lines)
        n (find-line-number "  Transaction Summary" lines)
        strs (-> lines
            (nth (+ n 2))
            (s/split #"[() ]"))]
    {:name (last strs)
     :buy-or-sell (first strs)}))

(defn contract-note-to-statement
  [transaction]
  (let [formatter (f/formatter "dd-MMM-yy")
       m (case (:buy-or-sell transaction) "Purchase" 1 "Sale" -1)]
    (-> {}
        (assoc :statement/trade-date (f/parse formatter
                                              (get transaction "Trade Date")))
        (assoc :statement/settlement-date (f/parse formatter
                                                   (get transaction "Settlement Date")))
        (assoc :statement/price (/ (quantity (get transaction "Price (p)")) 100))
        (assoc :statement/units (* m (quantity (get transaction "Quantity"))))
        (assoc :statement/product (get transaction :name))
        (assoc :statement/status "Confirmed")
        (assoc :statement/matched-status "Unmatched")
        (assoc :statement/broker "ATS"))))

(defn contract-note-transaction
  [file]
  (let [time-headers ["Trade Date" "Trade Time" "Settlement Date"]
        price-headers ["Quantity" "Price (p)" "Consideration"]
        times (extract-values file time-headers)
        prices (extract-values file price-headers)
        name (extract-security-name-and-buy-or-sell file)]
    (merge times prices name)))

(defn contract-note-statement
  [file]
  (-> file
      contract-note-transaction
      contract-note-to-statement))

(defn select-header
  [trs]
  (-> trs
      (html/select [:th :div])
      ((partial map (comp first :content)))))

(defn select-row
  [sel trs]
  (-> trs
      (html/select sel)
      ((partial map (comp first :content)))
      ((partial map s/trim))))

(defn select-data
  [sel trs]
  (-> trs
      (html/select [[:tr (html/has [:td])]])
      ((partial map (partial select-row sel)))))

(defn order-quantity-or-value
  [x]
  (if (.startsWith x "£")
    {:statement/value (- (value-in-£ x) 12.50M)}
    {:statement/units (- 0 (quantity x))}))

(defn order-to-statement
  [order]
  (let [status (get order "Status")]
    (if (contains? #{"Accepted" "Filled"} status)
      (-> (order-quantity-or-value (get order "Quantity/Value"))
          (assoc :statement/order-time (f/parse (f/formatter "dd/MM/yyyy HH:mm")
                                                (get order "Creation Time")))
          (assoc :statement/product (get order "Fund Symbol"))
          (assoc :statement/broker "ATS")
          (assoc :statement/currency "GBP")
          (assoc :statement/status "Open")
          (assoc :statement/matched-status "Unmatched")))))

(defn confirm-to-statement
  [confirm]
  (try
    (let [m (if (= "Sell" (get confirm "Trade Type")) -1 1)]
      (-> {}
          (assoc :statement/trade-date (f/parse (f/formatter "dd/MM/yyyy")
                                                (get confirm "Trade Date")))
          (assoc :statement/product (get confirm "Investment Symbol"))
          (assoc :statement/units (* m (quantity (get confirm "Quantity"))))
          (assoc :statement/price (value-in-£ (get confirm "Price")))
          (assoc :statement/value (* m (value-in-£ (get confirm "Consideration¹"))))
          (assoc :statement/broker "ATS")
          (assoc :statement/status "Partially Confirmed")
          (assoc :statement/matched-status "Unmatched")))
    (catch Exception e (throw (Exception. (str "could not process: " confirm))))))

(defn select-table
  [filename regex]
  (-> filename
      slurp
      html/html-snippet
      (html/select [[:table (html/has [(html/re-pred (re-pattern regex))])]])
      first))

(defn to-statement
  [data header f]
  (->> data
       (map #(map vector header %))
       (map (partial into {}))
       (map f)))

(defn html-transactions
  [table row-sel f-to-statement]
  (let [trs (html/select table [:tr])
        h   (select-header trs)
        ds  (select-data row-sel trs)]
    (to-statement ds h f-to-statement)))

(def currenex-smap
  {"Buy/Sell"    :none
   "CCY Pair"    :statement/product
   "CCY"         :statement/currency
   "Near Amount" :statement/units
   "Rate"        :statement/price
   "Trade Date Time (GMT)" :statement/fill-time})

(defn currenex-header?
  [header]
  (let [s (->> header
               (into #{}))
        hs (keys currenex-smap)]
    (reduce (fn [b h] (and b (contains? s h))) true hs)))

(defn csv-transactions
  [second-row-first? file]
  (let [lines (-> file
                  slurp
                  (string/split #"\r\n|\n"))
        rows (if second-row-first? (rest lines) lines)
        header (-> rows first (string/split #","))
        data   (->> rows rest (map #(string/split % #",")))]
    (->> data
         (map (partial map (partial vector) header))
         (map (partial into {})))))

(defn cqg-transaction?
  [transaction]
  (and
   (contains? transaction "Quantity")
   (contains? transaction "Fill Price Decimal")))

(defn cqg-transactions
  [file]
  (->> file
       (csv-transactions true)
       (filter cqg-transaction?)))

(defn currenex-transactions
  [file]
  (let [ts (csv-transactions false file)
        header (->> ts (map keys) flatten distinct)]
    (if (currenex-header? header)
      ts)))

(defn currenex-currency
  [pair-name]
  (-> pair-name
      (s/split #"/")
      second))

(defn currenex-to-statement
  [transaction]
  (let [dt (->> (get transaction "Trade Date Time (GMT)")
                (f/parse (f/formatter "dd MMM yy HH:mm:ss")))
        trade-date (c/to-local-date dt)
        m  (case (get transaction "Buy/Sell") "BUY" 1 "SELL" -1 :default (throw (Exception. "")))
        u (* m (bigdec (get transaction "Near Amount")))
        p (bigdec (get transaction "Rate"))
        n (get transaction "CCY Pair")]
    (-> {}
        (assoc :statement/product n)
        (assoc :statement/currency (currenex-currency n))
        (assoc :statement/units u)
        (assoc :statement/price p)
        (assoc :statement/value (* u p))
        (assoc :statement/fill-time dt)
        (assoc :statement/order-time dt)
        (assoc :statement/trade-date trade-date)
        (assoc :statement/settlement-date (f/parse (f/formatter "dd MMM yyyy")
                                                   (get transaction "Value Date")))
        (assoc :statement/broker "ABN")
        (assoc :statement/status "Filled")
        (assoc :statement/matched-status "Unmatched"))))

(defn cqg-to-statement
  [transaction]
  (let [n (get transaction "Contract")
        prod (-> n db/product-of)
        mult (-> prod :product/multiplier bigdec)
        ccy  (-> prod :product/currency)
        dt (->> (get transaction "Fill Time")
                (f/parse (f/formatter "dd/MM/yyyy HH:mm:ss")))
        trade-date (c/to-local-date dt)
        m (case (get transaction "Buy Or Sell") "b" 1 "s" -1 :default (throw (Exception. "")))
        u (* m (bigdec (get transaction "Quantity")))
        p (* mult (bigdec (get transaction "Fill Price Decimal")))]
    (-> {}
        (assoc :statement/product n)
        (assoc :statement/currency ccy)
        (assoc :statement/units u)
        (assoc :statement/price p)
        (assoc :statement/value (* u p))
        (assoc :statement/order-time dt)
        (assoc :statement/fill-time dt)
        (assoc :statement/trade-date trade-date)
        (assoc :statement/broker "ABN")
        (assoc :statement/status "Filled")
        (assoc :statement/matched-status "Unmatched"))))

(def abn-selectors
  {:future-transaction [:FutureMovement]
   :fx-transaction     [[:UnsettledMovement (html/has [:TransactionType #{(html/re-pred #"FX CONF")
                                                                          (html/re-pred #"FORWARD CONF")
;;                                                                          (html/re-pred #"EXPIRY")
                                                                          }])]]
   :equity-transaction [[:UnsettledMovement (html/has [:TransactionType (html/re-pred #"TRADE")])]]
   :cash-position [:CashPosition]
   :future-position [:FuturePosition]
   :fx-position [:UnsettledPosition]})

(defn all-abn-items
  [filename]
  (-> filename
      xml/parse
      (html/select [#{:Transactions :Positions}
                    #{[:AccountTransactions (html/attr= :AccountID "FCA-237-TRAD-2-1")]
                      [:AccountPositions (html/attr= :AccountID "FCA-237-TRAD-2-1")]}])))

(defn abn-items
  [filename]
  (-> filename
      all-abn-items
      (html/select [(->> abn-selectors vals (into #{}))])))

(defn select-field
  [xml selector]
  (->> [xml selector]
       (apply html/select)
       first
       :content
       first))

(defn select-units
  [x]
  (abs
   (bigdec
     (or
      (select-field x [:QuantityShort])
      (select-field x [:QuantityLong])))))

(defn abn-side-mult
  [x]
  (case (select-field x [:BuySellCode]) "SELL" -1 "BUY" 1))

(defn partial-statement
  "extracts common fields from the xml"
  [x]
  {:statement/broker "ABN"
   :statement/units (* (abn-side-mult x) (select-units x))
   :statement/currency (select-field x [:TransactionPriceCurrency :CurrencyCode])
   :statement/status "Confirmed"
   :statement/matched-status "Unmatched"
   :statement/trade-date (f/parse (f/formatters :basic-date)
                                  (select-field x [:TransactionDate]))})

(defn abn-type
  [xml']
  {:post (contains? abn-selectors %)}
  (->> abn-selectors
       (map (fn [[k s]] [k (boolean (seq (html/select xml' s)))]))
       (filter (fn [[k b]] b))
       first
       first))

(defmulti parse-abn abn-type)

(defmethod parse-abn :equity-transaction
  [xml']
  (let [sel (partial select-field xml')]
   (-> (partial-statement xml')
       (assoc :statement/product (sel [:Product :Symbol]))
       (assoc :statement/price (bigdec (sel [:TransactionPrice])))
       (assoc :statement/value (* (abn-side-mult xml') (bigdec (sel [:PrimaryAmount]))))
       (assoc :statement/internal-reference (sel [:InternalReference])))))

(defmethod parse-abn :future-transaction
  [xml']
  (let [sel (partial select-field xml')
        mult (int (bigdec (sel [:Tradingunit])))
        p (* mult
             (bigdec (sel[:TransactionPrice])))
        symbol (sel [:Product :Symbol])
        expiry (f/parse (f/formatters :basic-date) (sel [:Product :Expiry]))
        currency (sel [:TransactionPriceCurrency :CurrencyCode])
        product (a/fu {:product/underlying-product symbol
                       :product/expiry (db/expiry-month expiry)
                       '(int :product/multiplier) mult
                       :product/currency currency})]
    (-> xml'
        partial-statement
        (assoc :statement/product product)
        (assoc :statement/price p)
        (assoc :statement/value (* (abn-side-mult xml') p (select-units xml')))
        (assoc :statement/internal-reference (sel [:InternalFutureReference]))
        (assoc :statement/settlement-date (f/parse (f/formatters :basic-date)
                                                   (sel [:Expiry]))))))

(defmethod parse-abn :fx-transaction
  [xml']
  (let [sel (partial select-field xml')
        v (bigdec (sel [:PrimaryAmount]))
        u (select-units xml')
        p (partial-statement xml')
        s (if (sel [:SettlementDate]) (f/parse (f/formatters :basic-date)
                                                          (sel [:SettlementDate])))
        p' (if s (assoc p :statement/settlement-date s) p)]
   (-> p'
       (assoc :statement/product (str (sel [:CounterValue :ValueCur])
                                      "/"
                                      (sel [:PrimaryAmountCurrency :CurrencyCode])))
       (assoc :statement/price (/ v u))
       (assoc :statement/value (* (abn-side-mult xml')
                                  v))
       (assoc :statement/internal-reference (sel [:InternalReference]))
       (assoc :statement/external-reference (sel [:ExternalNumber])))))

(defn abn-change-transaction
  [xml']
  (let [sel (partial select-field xml')
        side (sel [:CashPositionChange :ValueDC])
        mult (case side "D" 1 "C" -1)
        units (* mult (bigdec (sel [:CashPositionChange :Value])))
        date (f/parse (f/formatters :basic-date) (sel [:ProcessingDate]))]
    (if (pos? (abs units))
      {:statement/trade-date date
       :statement/settlement-date date
       :statement/currency (sel [:Currency :CurrencyCode])
       :statement/units units
       :statement/value units
       :statement/price 1M
       :statement/product (sel [:CashAmountDescription])
       :statement/broker "ABN"
       :statement/status "Confirmed"
       :statement/matched-status "Unmatched"})))

(defn processing-date
  [xml']
  (f/parse (f/formatters :basic-date)
           (select-field xml' [:ProcessingDate])))

(def interests
  ["ACCRUED INTEREST CASH"
   "ACCRUED INTEREST MARGIN"
   "INTEREST EXPENSE CASH"
   "INTEREST EXPENSE MARGIN"])

(def fees
  ["SUB-CLEARER/BROKER   FEE TRADE Futures"
   "EUREX TRADING FEE Futures"
   "US TRADING FEE Futures"
   "CLEARING FEE TRADE Others"
   "US CCP FEE Futures"
   "CLEARING FEE TRADE Futures"
   "NFA FEE Futures"])

(defmethod parse-abn :cash-position
  [xml']
  (let [change? (seq
                 (html/select xml'
                              [[:CashPosition
                                (->> interests
                                     (map re-pattern)
                                     (map html/re-pred)
                                     (map vector)
                                     (map html/has)
                                     (into #{}))]]))
        sel (partial select-field xml')
        m (case (sel [:CashPositionNew :ValueDC]) "D" -1 "C" 1)
        category (sel [:CashAmountDescription])
        currency (sel [:CashPositionNew :ValueCur])
        units (bigdec (sel [:CashPositionNew :Value]))]
    [(if change? (abn-change-transaction xml'))
     (if (pos? (abs units))
       {:cash-position/time (processing-date xml')
        :cash-position/units (* m units)
        :cash-position/category category
        :cash-position/currency currency})
     (if (and (not= "EUR" currency)
              (sel [:CurrencyPrice]))
       {:price/value (bigdec (sel [:CurrencyPrice]))
        :price/product (str currency ".EUR")
        :price/time (processing-date xml')
        :price/aspect :price.aspect/settlement
        :price/source :price.source/abn})]))

(defmethod parse-abn :fx-position
  [xml']
  (let [sel (partial select-field xml')
        product (sel [:Product :Symbol])
        expiry (sel [:Product :Expiry])
        units (or (sel [:QuantityLong])
                  (sel [:QuantityShort]))]
    (if (and expiry units)
      {:position/time (processing-date xml')
       :position/units units
       :position/product product
       :position/currency (sel [:ValuationPriceCurrency :CurrencyCode])
       :position/settlement-date (f/parse (f/formatters :basic-date) (sel [:Product :Expiry]))})))

(defmethod parse-abn :future-position
  [xml']
  (let [sel (partial select-field xml')
        prod-def {:product/underlying-product (sel [:Product :Symbol])
                  '(int :product/multiplier) (int (bigdec (sel [:Tradingunit])))
                  :product/expiry (db/expiry-month
                                   (f/parse (f/formatters :basic-date)
                                            (sel [:Product :Expiry])))}]
    {:position/time (processing-date xml')
     :position/currency (sel [:Currency :CurrencyCode])
     :position/units (let [s (sel [:QuantityShort])]
                       (if s
                         (- 0 (bigdec s))
                         (bigdec (sel [:QuantityLong]))))
     :position/product (a/fu prod-def)}))

(defn file-type
  [file]
  (cond
   (try (select-table file "Order ID")
        (catch Exception e nil))
   :ats-order
   (try (select-table file "Trade Date")
        (catch Exception e nil))
   :ats-confirm
   (try (first (currenex-transactions file))
        (catch Exception e nil))
   :currenex
   (try (first (cqg-transactions file))
        (catch Exception e nil))
   :cqg
   (try (first (all-abn-items file))
        (catch Exception e nil))
   :abn
   (try (contract-note-transaction file)
        (catch Exception e nil))
   :ats-contract))

(defmulti parse file-type)

(defmethod parse :ats-order
  [file]
  (-> file
      (select-table "Order ID")
      (html-transactions [:td] order-to-statement)))

(defmethod parse :ats-confirm
  [file]
  (-> file
      (select-table "Trade Date")
      (html-transactions [:td :div] confirm-to-statement)))

(defmethod parse :currenex
  [file]
  (->> file
       currenex-transactions
       (map currenex-to-statement)))

(defmethod parse :cqg
  [file]
  (->> file
       cqg-transactions
       (map cqg-to-statement)))

(defmethod parse :abn
  [file]
  (->> file
       abn-items
       (map parse-abn)
       flatten))

(defmethod parse :ats-contract
  [file]
  [(contract-note-statement file)])

(defmethod parse :default
  [file]
  (throw (Exception. "unknown file type")))

(defn assoc-file-name
  [file datom]
  (if (= :statement (ec/entity-prefix datom))
    (assoc datom :statement/file-name (str file))
    datom))

(defn statements
  [file]
  (->> file
       parse
       flatten
       (filter #(not (nil? %)))
       distinct
       (map (partial assoc-file-name file))))

(defn save-statements!
  [file]
  (try (let [sts (statements file)]
         (if (seq sts)
           @(a/save! sts (:statement db/default-keys))
           true))
       (catch Exception e (do (pprint e) false))))

(comment
  (statements (File. "./docs/archive/processed/currenex.csv"))
  )
