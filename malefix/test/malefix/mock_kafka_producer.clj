(ns malefix.mock-kafka-producer
  (:require [com.stuartsierra.component :as component]
            [malefix.message-dispatcher :as md]
            [clojure.core.async :as async :refer [>!! <!! chan]]
            [clojure.tools.logging :as log]
            [malefix.message-dispatcher :as md]))

(defrecord MockKafkaProducer [channel-size channel]
  component/Lifecycle
  (start [mock-kafka-producer]
    (log/info "Starting mock kafka producer...")
    (assoc mock-kafka-producer :channel (chan channel-size)))
  (stop [mock-kafka-producer]
    (log/info "Stopping mock kafka producer..."))

  md/MessageDispatcher
  (send-message
    [mock-kafka-producer message topic]
    (log/info "Placing message on mock kafka channel" topic message)
    (>!! (:channel mock-kafka-producer) {:topic topic, :message message})))

(defn new-producer
  [channel-size]
  (map->MockKafkaProducer {:channel-size channel-size}))

(defn read-message
  [mock-kafka-producer]
  (<!! (:channel mock-kafka-producer)))
