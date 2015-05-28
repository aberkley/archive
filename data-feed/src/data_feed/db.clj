(ns data-feed.db
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defrecord Database [host port connection]
  ;; Implement the Lifecycle protocol
  component/Lifecycle

  (start [component]
    (log/info "Starting database...")
    (let [conn nil]
      (assoc component :connection conn)))

  (stop [component]
    (log/info "Stopping database")
    (assoc component :connection nil)))

(defn new-database [host port]
  (map->Database {:host host :port port}))
