(ns elsa.scheduler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.cron :refer [schedule cron-schedule]]
            [clojurewerkz.quartzite.conversion :as qc]))

(import '(java.io PushbackReader))

(defrecord Scheduler [scheduler]
  component/Lifecycle
  (start [this]
    (log/info "Starting quartz scheduler...")
    (if-let [s (-> (qs/initialize) qs/start)]
      (do
        (log/info "Got scheduler" (str s))
        (assoc this :scheduler s))
      (throw (RuntimeException. "Could not start quartz!"))))
  (stop [this]
    (log/info "Stopping quartz scheduler")
    (qs/shutdown scheduler)))4

(defn schedule-update-task
  "Schedules a cron task with the underlying quartz scheduler."
  [this job-fn job-data cron-line]
  (let [scheduler (:scheduler this)
        job (j/build
             (j/of-type job-fn)
             (j/using-job-data job-data)
             (j/with-identity (j/key (format "%s.job" job-fn))))
        trigger (t/build
                 (t/with-identity (t/key (format "%s.cron" job-fn)))
                 (t/start-now)
                 (t/with-schedule (schedule (cron-schedule cron-line))))]
    (log/info "Using scheduler" scheduler)
    (qs/schedule scheduler job trigger)))
