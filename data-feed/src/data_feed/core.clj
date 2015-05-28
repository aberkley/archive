(ns data-feed.core
  (:require [compojure.core :refer [defroutes GET]]
            [ring.middleware.params :refer [wrap-params]]
            [liberator.core :refer [resource defresource]]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [data-feed.db :as db]))

;; # Data Feed REST API


(defonce sample-schedule
  {:bloomberg {:daily
               {:cron-schedule "0 0/1 * * * ?"}
               :daily-slice
               {:cron-schedule "0 0/1 * * * ?"}}
   :deutche-website {:daily
                     {:cron-schedule "0 0/1 * * * ?"}}})

(defonce sample-symbols
  {:bloomberg {:daily
               {:symbols ["TPX Index" "SPX Index"]}
               :daily-slice
               {:symbols ["GBPUSD Curncy" "GBPJPY Curncy"]
                :time "12:00:00"
                :zone "Europe/London"}}
   :deutche-website {:daily
                     {:symbols ["DBLCI DIVERSIFIED COMMODITY INDEX BASKET COMMODITY INDEX"
                                    "DBLCI DIVERSIFIED AGRICULTURE INDEX BASKET COMMODITY INDEX"
                                    "DBIQ OPTIMUM YIELD INDUSTRIAL METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"
                                    "DBIQ OPTIMUM YIELD PRECIOUS METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"]}}})

;; Generates (as EDN) a schedule for ELSA to use to know which data to parse
(defresource schedule [version source]
  :available-media-types ["text/plain" "application/edn"]
  :exists? (fn [_] (= 1 (Integer/parseInt version)))
  :handle-ok (fn [_] (str (sample-schedule (keyword source)))))

(defresource symbols [version source]
  :available-media-types ["text/plain" "application/edn"]
  :exists? (fn [_] (= 1 (Integer/parseInt version)))
  :handle-ok (fn [_] (str (sample-symbols (keyword source)))))

(defroutes app
  (GET "/data-feed/:version/:source/schedule" [version source] (schedule version source))
  (GET "/data-feed/:version/:source/symbols" [version source] (symbols version source)))

(def handler
  (-> app
      wrap-params))

(defn data-feed-system
  "This sytem specifies all the component dependencies that comprise Malefix"
  [config-options]
  (let [{:keys [host port]} config-options]
    (component/system-map
     :db (db/new-database host port))))

(defn init
  "Starts up dependency injection and wires up a database component"
  []
  (log/info "Starting up component system...")
  (def system (data-feed-system {:host "localhost", :port 9000}))
  (alter-var-root #'system component/start))

(defn shutdown
  "Shuts down the system, and exits"
  [reason exit-code]
  (log/info "Shutting down with reason: " reason)
  (alter-var-root #'system component/stop)
  (System/exit exit-code))
