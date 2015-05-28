(ns utilities.core-test
  (:require [clojure.test :refer :all]
            [utilities.core :refer :all]))

(deftest a-test
  (is (boolean (parse-instant "14-Oct-2014")))
  (is (boolean (parse-instant "14/10/2014 10:33:11")))
  (is (boolean (parse-instant "14/10/2014 10:33"))))
