(ns malefix.test-helpers
  (:require [midje.sweet :refer :all]
            [clojure.tools.logging :as log])
  (:import [quickfix.fix44 NewOrderSingle]
           [quickfix.field ClOrdID Side Symbol OrdType TransactTime]
           [java.util Date]))

(defn send-nos
  [session times]
  (dotimes [i times]
    (let [nos (NewOrderSingle.)
          clordid (ClOrdID. (str "order" i))
          side (Side. \1)
          symbol (Symbol. (str "symbol" i))
          ordtype (OrdType. \1)
          transacttime (TransactTime. (Date.))]
      (doto nos
        (.set clordid)
        (.set side)
        (.set symbol)
        (.set ordtype)
        (.set transacttime))
      (.send session nos)
      (log/info "Sent NOS" nos))))
