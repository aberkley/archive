(ns utilities.html-test
  (:require [clojure.test :refer :all]
            [utilities.html :refer :all]))

(deftest a-test
  (is (html-table [{:a 1}])))
