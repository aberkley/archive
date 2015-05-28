(ns back-office.crossrate
  (:require [entomic.api :as a]
            [db.core :as db]))

(defn crossrate-of
  [c1 c2]
  (or (db/product-of (str c1 "." c2))
      (db/product-of (str c1 "/" c2))
      (db/product-of (str c1 c2 " Curncy"))))

(defn bb-currency-of
  [cr]
  (let [cs (seq (clojure.string/split cr " Curncy"))
        n (int (/ (count seq) 2))
        c1 (str (take n cs))
        c2 (str (drop n cs))]
    (if (and (db/product-of c1)
             (db/product-of c2))
      [c1 c2])))

(defn currencies-of
  ([cr]
     (or (currencies-of #"\." cr)
         (currencies-of #"/" cr)
         (bb-currency-of)))
  ([re cr]
     (let [[c1 c2] (clojure.string/split cr re)]
       (if (and (db/product-of c1)
                (db/product-of c2))
         [c1 c2]))))

(defn abn-fx-rate
  [c1 c2 dt]
  (cond
   (= "EUR" c2) (:price/value (a/fu {:price/product (crossrate-of c1 c2) :price/time dt}))
   (= c1 c1)    1
   :else (let [cr1 (crossrate-of c1 "EUR")
               cr2 (crossrate-of c2 "EUR")
               fx1 (a/fu {:price/product cr1 :price/time dt})
               fx2 (a/fu {:price/product cr2 :price/time dt})]
           (if (and fx1 fx2)
             (/ (float (:price/value fx1))
                (float (:price/value fx2)))))))
