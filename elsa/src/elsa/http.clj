(ns elsa.http
  (:require [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]))

(defn retrieve-config
  "Gets the body of the specified type/source HTTP request as edn"
  [base-url source type]
  (let [full-url (format "%s/%s/%s" base-url (name source) (name type))]
    (log/info "retrieving config from" full-url)
    (if-let [data (client/get full-url)]
      (edn/read-string (:body data))
      (throw (RuntimeException. "Did not retrieve config from URL")))))
