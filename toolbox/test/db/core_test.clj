(ns db.core-test
  (:require [clojure.test :refer :all]
            [db.core :as d]
            [entomic.core :as e]
            [entomic.api :as a]
            [entomic.coerce]
            [entomic.format :as f]
            [datomic.api]))

(e/resolve-api! (find-ns 'datomic.api))

(def uri "datomic:mem://test")

;;(def uri d/db-uri)

(e/delete-database uri)

(e/create-database uri)

(e/set-connection! uri)

(d/initialise!
 (find-ns 'entomic.core)
 (find-ns 'entomic.format)
 (find-ns 'entomic.api)
 (find-ns 'entomic.coerce))

(deftest entomic-test
  (is (d/save-new-products! [{:synonym/name "TPX Index"}] [{:product/type "Index"}]))
  (is (a/save! [{:statement/broker "ATS" :statement/product "TPX Index"}]))
  (is (:synonym/name (d/fu :synonym)))
  (is (:product/type (d/product-of "TPX Index")))
  (is (f/set-custom-unparser! [:statement/product] d/primary-name-of))
  (is (= "TPX Index" (:statement/product (a/fu :statement)))))

(comment
  (e/find-ids {:statement/product {:product/type "Index"}})
  (f/parse-entity {:statement/product {:product/type "Index"}})
  (a/f {:statement/product {:product/type "Index"}})
  (d/primary-name-of (a/fu :product))
  )
