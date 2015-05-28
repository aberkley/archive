(ns data-feed.core-test
  (:require [data-feed.core :refer :all]
            [clojure.edn :as edn]
            [clojure.test :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer :all]
            [ring.mock.request :as mock]))

(with-test
  (defn get-schedule
    [source]
    (->> (format "/data-feed/1/%s/schedule" (name source))
         (mock/request :get)
         handler
         :body
         edn/read-string))

  (is (= (:bloomberg sample-schedule) (get-schedule :bloomberg)))
  (is (= (:deutche-website sample-schedule) (get-schedule :deutche-website))))
