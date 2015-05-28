(ns malefix.core-test
  (:use midje.sweet)
  (:require [malefix.core :refer :all]
            [malefix.quickfix :as fix]
            [malefix.mock-kafka-producer :as mock]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [malefix.test-helpers :refer :all])
  (:import [quickfix Session]
           [ch.qos.logback.classic Level Logger]
           [org.slf4j LoggerFactory]))

(defonce initiator "config/initiator.edn")
(defonce acceptor "config/acceptor.edn")

(defn malefix-test-system
  "Defines a test system with two fix engine components"
  [acceptor-options initiator-options]
  (let [producer-config (:producer-config acceptor-options)
        acceptor-config (:qfj-config acceptor-options)
        initiator-config (:qfj-config initiator-options)]
    (component/system-map
     :producer (mock/new-producer 100)
     :initiator (fix/new-fix-engine initiator-config :initiator)
     :acceptor (component/using
                (fix/new-fix-engine acceptor-config :acceptor)
                [:producer]))))

(defn set-up
  "Loads our acceptor and initiator configs and spawns a test System"
  []
  (.setLevel (LoggerFactory/getLogger (Logger/ROOT_LOGGER_NAME)) (Level/INFO))
  (let [acceptor-options (read-config acceptor)
        initiator-options (read-config initiator)]
    (def test-system (malefix-test-system acceptor-options initiator-options))
    (alter-var-root #'test-system component/start))
  (while (not (and (.isLoggedOn (get-in test-system [:acceptor :session-connector]))
                   (.isLoggedOn (get-in test-system [:initiator :session-connector]))))
    (do
      (log/info "Waiting for logon...")
      (Thread/sleep 1000)))
  (let [session (Session/lookupSession
                 (->> [:initiator :session-connector]
                      (get-in test-system)
                      .getSessions
                      first))]
    (send-nos session 10)))

(defn tear-down
  []
  (alter-var-root #'test-system component/stop))

(defn next-clordid
  []
  (->> [:acceptor :producer]
       (get-in test-system)
       mock/read-message
       :message
       .getClOrdID
       .getValue))

(with-state-changes
  [(before :facts (set-up) :after (tear-down))]
  ;; This integration test uses two FIX engines and a Kafka topic to ensure
  ;; that end to end communications are flowing as intended
  (fact "End to end messages are flowing through quickfix and kafka"
        (.isLoggedOn (get-in test-system [:acceptor :session-connector])) => true
        (.isLoggedOn (get-in test-system [:initiator :session-connector])) => true
        (next-clordid) => "order0"
        (next-clordid) => "order1"
        (next-clordid) => "order2"
        (next-clordid) => "order3"
        (next-clordid) => "order4"
        (next-clordid) => "order5"
        (next-clordid) => "order6"
        (next-clordid) => "order7"
        (next-clordid) => "order8"
        (next-clordid) => "order9"))
