(ns back-office.database
  (:require [db.core :as db]
            [entomic.core :as e]
            [entomic.api :as a]
            [entomic.coerce]
            [entomic.format]
            [datomic.api :as d]))

(defn init-db! []
  (e/resolve-api! (find-ns 'datomic.api))
  (e/set-connection! db/db-uri)
  (db/initialise!
   (find-ns 'entomic.core)
   (find-ns 'entomic.format)
   (find-ns 'entomic.api)
   (find-ns 'entomic.coerce)))

(comment
  (init-db!)
  (->> (a/f :statement)
       (partition-all 50)
       (map a/retract-entities!))

  (db/product-of "EUR.USD")

  (a/fu {:synonym/name "EUR.USD"})

  (save-new-products! [{:synonym/name "USD.EUR"} {:synonym/name "JPY.EUR"} {:synonym/name "GBP.EUR"}]
                      [{:product/type "CROSS"} {:product/type "CROSS"} {:product/type "CROSS"}])

  (a/f {:position/currency "JPY" :position/product "GBP/JPY"})

  (retract-entities
   (entities
    (db conn)
    (d/q
     '[:find ?e
       :where
       [?e :db/fn]]
     (db conn))))


  (def t-prods (->> (find :transaction)
                    (map :transaction/product)
                    (map :db/id)
                    distinct
                    (into #{})))

  (->> (find {:product/type "Future"})
       (filter (complement #(contains? t-prods (:db/id %))))
       retract-entities)

  (->> (find {:product/underlying-product "TPX Index"})
       (map bb-name-of))

  (save! [{:synonym/product 17592186045594
            :synonym/name "TMIU4 Index"
            :synonym/type :synonym.type/ticker}])

  (map bb-name-of t-prods)

  (save-new-products!
   [{:synonym/name "DBLCI DIVERSIFIED COMMODITY INDEX BASKET COMMODITY INDEX"}
    {:synonym/name "DBLCI DIVERSIFIED AGRICULTURE INDEX BASKET COMMODITY INDEX"}
    {:synonym/name "DBIQ OPTIMUM YIELD INDUSTRIAL METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"}
    {:synonym/name "DBIQ OPTIMUM YIELD PRECIOUS METALS INDEX EXCESS RETURN BASKET COMMODITY INDEX"}]
     [{:product/type "Index" :product/currency "USD"}
      {:product/type "Index" :product/currency "USD"}
      {:product/type "Index" :product/currency "USD"}
      {:product/type "Index" :product/currency "USD"}])

  (find {:synonym/name "W N9 Comdty"})
  (save-new-products! [{:synonym/name "W N9 Comdty"}] [{:product/type "Future"}])

  (let [e (d/entity (db conn) :product/underlying)
        id (:db/id e)
        e' (-> (zipmap (keys e) (vals e))
               identity
               (assoc :db/valueType :db.type/ref)
               (assoc :db/id id)
               vector)]
    (d/transact conn e'))

  (->> (find :product)
       (filter :product/underlying)
       (map #(assoc % :product/underlying-product (:db/id (product-of (:product/underlying %)))))
       doall
       update!
       )


  (->> (find :synonym)
       (filter (complement :synonym/product))
       (map :db/id)
       (map (fn [id] `[:db.fn/retractEntity ~id]))
       (d/transact conn))
  (count
   (find :product))

  (->> (find :holding)
       (map :db/id)
       (map (fn [id] `[:db.fn/retractEntity ~id]))
       (d/transact conn))

  (find {:product/underlying-product "SPX Index"})

  (->> (find :product)
       (filter :product/underlying)
       ;;(map #(update-in % [:product/underlying-product] :db/id))
       (retract :product/underlying)
       )

  (->> (find :product)
       (retract :product/mult))
  (->> (find :product)
       (filter :product/mult)
       (map #(assoc % :product/multiplier (bigdec (:product/mult %))))
       update!)
  )
