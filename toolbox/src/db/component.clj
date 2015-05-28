(ns db.component
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(declare connect-database)

(defrecord Database [connection-url connection]
  component/Lifecycle
  (start [this]
    (log/info "Initialising database connection with: " connection-url)
    (if-let [conn (connect-database connection-url)]
      (assoc this :connection conn)
      (throw (RuntimeException. "Could not establish database connection"))))
  (stop [this]
    (log/info "Shutting down database connection.")
    ;; Cleanly close the database connection
    ))

(defn new-database
  [connection-url]
  (map->Database {:connection-url connection-url}))

(defn connect-database
  [connection-url]
  (log/info "I don't do anything yet!")
  ;; Connect the database
  )

(defn query
  "Query the database component"
  [this query]
  (let [conn (:connection this)]
    (log/info "Doing a query!")
    ;; Do a query
    ))
