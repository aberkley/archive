(ns elsa.bb-data-loader
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.conversion :as qc]
            [elsa.scheduler :as s]
            [elsa.http :as http]))


(defjob daily-job
  [ctx]
  (let [m (qc/from-job-data ctx)
        base-url (m "base-url")
        symbols (http/retrieve-config base-url :bloomberg :symbols)
        daily-symbols (get-in symbols :daily :symbols)]
    (log/info "Daily job invoked with" daily-symbols)))

(defjob daily-slice-job
  [ctx]
  (let [m (qc/from-job-data ctx)
        base-url (m "base-url")
        symbols (http/retrieve-config base-url :bloomberg :symbols)
        daily-slice-symbols (get-in symbols :daily-slice :symbols)]
    (log/info "Daily slice job invoked with" daily-slice-symbols )))



(defrecord BloombergDataLoader [scheduler base-url]
  component/Lifecycle
  (start [this]
    (log/info "Starting Bloomberg Data Loader component")
    (let [config (http/retrieve-config base-url :bloomberg :schedule)
          daily (:daily config)
          daily-cron-schedule (:cron-schedule daily)
          daily-slice (:daily-slice config)
          daily-slice-cron-schedule (:cron-schedule daily-slice)]
      (log/info "Scheduling daily task with config" daily)
      (s/schedule-update-task scheduler
                              daily-job
                              {:base-url base-url}
                              daily-cron-schedule)
      (log/info "Scheduling daily slice task with config" daily)
      (s/schedule-update-task scheduler
                              daily-slice-job
                              {:base-url base-url}
                              daily-slice-cron-schedule)))
  (stop [this]
    (log/info "Stopping Bloomberg Data Loader component")))

(defn new-bloomberg-data-loader
  [base-url]
  (map->BloombergDataLoader {:base-url base-url}))
