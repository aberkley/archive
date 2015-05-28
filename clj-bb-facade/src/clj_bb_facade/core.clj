(ns clj-bb-facade.core
  (:import [com.bloomberglp.blpapi Session SessionOptions CorrelationID]))

(def api-constructors
  "real 3.6.1"
  {:new-session-options (fn [] (SessionOptions.))
   :new-session (fn [session-options] (Session. session-options))
   :new-correlation-id (fn [id] (CorrelationID. id))})
