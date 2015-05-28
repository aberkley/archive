(ns malefix.quickfix
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [malefix.message-dispatcher :refer [send-message]]))

(import '(quickfix Application SessionSettings FileStoreFactory FileLogFactory DefaultMessageFactory SocketAcceptor SocketInitiator))

;; # Quickfix/J
;;
;; Quickfix/J is an open source Java FIX engine. Its API is a little... Java-ish.
;; The functions in this namespace wrap the Java API calls in Clojure fns with
;; the intention of making it a little bit more pleasant to work with in Java code.

(declare start-initiator start-acceptor)

;; The FixEngine component represents the top-level acceptor or initiator instances we need
;; to connect to or receive connections from FIX
(defrecord FixEngine [qfj-config direction session-connector logged-on? producer]
  component/Lifecycle
  (start [fix-engine]
    (log/info "Starting FIX engine component for" qfj-config direction)
    (assoc fix-engine :session-connector
           (case direction
             :initiator (start-initiator fix-engine qfj-config)
             :acceptor  (start-acceptor fix-engine qfj-config)
             (throw (RuntimeException.
                     "Unknown direction passed to FixEngine, not starting anything")))))
  (stop [fix-engine]
    (log/info "Stopping FIX engine component")
    (.stop (:session-connector fix-engine))))

(defn new-fix-engine
  "Builds a FixEngine component with no state initially"
  [qfj-config direction]
  (map->FixEngine {:qfj-config qfj-config, :direction direction}))

;; ### Using Apache Kafka as a durable queue
(defn make-channel-application
  "Builds a default implementation of Application that writes FIX messages to a Kafka queue"
  [fix-engine]
  (reify Application
    (fromApp [application fix-message session-id]
      (send-message (:producer fix-engine) fix-message session-id))
    (toApp [application fix-message session-id])
    (onCreate [application session-id])
    (onLogon [application session-id]
      (log/info "************* Session logged on" session-id))
    (onLogout [application session-id]
      (log/info "************* Session logged out" session-id))
    (toAdmin [application fix-message session-id])
    (fromAdmin [application fix-message session-id])))

;; Then we start the main QFJ instance. All handling of messages is done by passing in
;; a reification of the Application interface. All processing and onward handling happens from there.
;; Currently we're using a default application which does nothing.

(defn start-initiator
  "Bootstraps a Quickfix/J initiator instance with the supplied config"
  [fix-engine file-path]
  (let [application (make-channel-application fix-engine)
        settings-file (io/file file-path)
        settings-stream (io/input-stream settings-file)
        settings (SessionSettings. settings-stream)
        store-factory (FileStoreFactory. settings)
        log-factory (FileLogFactory. settings)
        message-factory (DefaultMessageFactory.)
        initiator (SocketInitiator. application store-factory settings log-factory message-factory)]
    (log/info "Starting using settings: " \newline (str settings))
    (.start initiator)
    initiator))

(defn start-acceptor
  "Bootstraps a Quickfix/J acceptor instance with the supplied config"
  [fix-engine file-path]
  (let [application (make-channel-application fix-engine)
        settings-file (io/file file-path)
        settings-stream (io/input-stream settings-file)
        settings (SessionSettings. settings-stream)
        store-factory (FileStoreFactory. settings)
        log-factory (FileLogFactory. settings)
        message-factory (DefaultMessageFactory.)
        acceptor (SocketAcceptor. application store-factory settings log-factory message-factory)]
    (log/info "Starting using settings: " \newline (str settings))
    (.start acceptor)
    acceptor))
