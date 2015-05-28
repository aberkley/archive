(ns malefix.kafka-producer
  (:require [clj-kafka.producer :as kafka]
            [clj-kafka.zk :refer [brokers]]
            [clojure.tools.logging :as log]
            [malefix.message-producer :as mp]
            [com.stuartsierra.component :as component]))

(defrecord KafkaProducer [config producer]
  component/Lifecycle
  (start [kafka-producer]
    (log/info "Starting Kafka producer with config:" config)
    (if-let [new-producer (kafka/producer config)]
      (assoc kafka-producer :producer new-producer)
      (throw (RuntimeException. "Could not instantiate producer."))))
  (stop [kafka-producer]
    (log/info "Shutting down producer...")
    (assoc kafka-producer :producer nil))

  mp/MessageProducer
  (send-message
    [message-producer message topic]
    (kafka/send-message (:producer message-producer) (->> message
                                              str
                                              .getBytes
                                              (kafka/message topic)))))

(defn new-producer
  [config]
  (map->KafkaProducer {:config config}))
