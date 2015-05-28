(ns back-office.handler
  (:use [compojure.core]
        [clojure.pprint]
        [ring.util.response]
        [ring.middleware.params]
        [clojure.set :only [rename-keys]]
        [back-office.scheduled])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [utilities.html :as h]
            [db.core :as db]
            [entomic.api :as a]
            [back-office.scheduled :as scheduled]))

;; type "lein ring server-headless 3000"

(defn format-statements
  [sts]
  (->> sts
       (sort-by #(or (:statement/trade-date %) (:statement/order-time %)))
       (map #(update-in % [:statement/product] db/display-name-of))
       h/html-table))

(defn status-string
  [b]
  (if b
    "OK"
    "Failing"))

(defn status-summary
  []
  (let [failed? (boolean (seq (-> (all-files) failed-summary)))
        unmatched? (a/f? {:statement/matched-status "Unmatched"})]
    [{:process "file processing" :status (status-string (not failed?))}
     {:process "statement matching" :status (status-string (not unmatched?))}]))

(defroutes app-routes
  (GET "/directory-summary" [] (h/html-table (deref scheduled/directory-summary)))
  (GET "/unmatched-statements" [] (format-statements (a/f {:statement/matched-status "Unmatched"})))
  (GET "/status" [] (h/html-table (status-summary))))

(def app
  (wrap-params
   (handler/site app-routes)))
