(ns benefix.kafka
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]))

(defrecord KafkaConsumer [kafka-config]
  component/Lifecycle

  (start [this]
    (log/info "Starting Kafka consumer..."))

  (stop [this]
    (log/info "Stopping Kafka consumer")))

(defn new-kafka-consumer [kafka-config]
  (map->KafkaConsumer {:kafka-config kafka-config}))
