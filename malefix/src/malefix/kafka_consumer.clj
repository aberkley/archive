(ns malefix.kafka-consumer
  (:require [clj-kafka.consumer.zk :as kafka]
            [clj-kafka.zk :refer [brokers]]
            [clojure.tools.logging :as log]
            [malefix.message-consumer :as mc]
            [com.stuartsierra.component :as component]))

(defrecord KafkaConsumer [config consumer]
  component/Lifecycle
  (start [kafka-consumer]
    (log/info "Starting Kafka consumer with config:" config)
    (if-let [new-consumer (kafka/consumer config)]
      (assoc kafka-consumer :consumer new-consumer)
      (throw (RuntimeException. "Could not instantiate producer."))))
  (stop [kafka-consumer]
    (log/info "Shutting down producer...")
    (assoc kafka-consumer :consumer nil))
  mc/MessageConsumer
  (read-message [kafka-consumer topic]
    (first (kafka/messages (:consumer kafka-consumer) topic))))
