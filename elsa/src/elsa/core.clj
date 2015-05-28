(ns elsa.core
  (:use clojure.pprint
        utilities.core
        clojure.set)
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [elsa.scheduler :as scheduler]
            [clj-bb-api.core :as api]
            [clj-http.client :as client]
            [elsa.bb-data-loader :as bb]))

(defrecord DataLoader [data-feed-url scheduler]
  component/Lifecycle
  (start [component] component)
  (stop [component] component))

(defn bloomberg-requests [config]
  [[api/get-historical-data "20140101" "20150101" ["TPX Index" "GBPUSD Curncy"]]
   ;;...
   ])

(defn on-scheduled-event [{:keys [data-feed-url] :as this}]
  (let [slurped-config (client/get data-feed-url {:as :auto})
        requests (bloomberg-requests slurped-config)
        fs (map first requests)
        args (map last requests)
        data (map (fn [f args'] (apply f args)) fs args)
        ]
    (client/put "some save data url" {:data data})))



(import java.io.PushbackReader)

(defonce config-edn "config/default.edn")

(declare shutdown)

(defn elsa-system
  "This sytem specifies all the component dependencies that comprise Elsa"
  [config-options]
  (component/system-map
   :scheduler (scheduler/map->Scheduler {})
   :bb-data-loader (component/using
                    (bb/new-bloomberg-data-loader (:base-url config-options))
                    [:scheduler])))

(defn -main
  "Entrypoint for ELSA. Starts up a component system using an edn config"
  []
  (log/info "Loading default config")
  (try
    (with-open [config-reader (PushbackReader. (io/reader config-edn))]
     (log/info "Starting with config options found at" config-edn)
     (def system (elsa-system (edn/read config-reader) ))
     (alter-var-root #'system component/start))
    (catch Exception e (shutdown (str (.getMessage e) ", cause: " (.getCause e)) 1)))
  (log/info "Scheduling update task"))

(defn- shutdown
  "Shuts down the system, and exits"
  [reason exit-code]
  (log/info reason)
  (alter-var-root #'system component/stop)
  (System/exit exit-code))
