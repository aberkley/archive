(ns utilities.html
  (:require [net.cgrand.enlive-html :as html]))

(def data-snippet (-> "<table><tr class=\"header\"><th>Header<tr class=\"data\"><td>Data"
                      java.io.StringReader.
                      html/html-resource))

(defn data-row
  [ys hs]
  {:pre (= (count ys) (count hs))}
  (let [y-hs (map vector ys hs)]
    (-> data-snippet
        (html/select [:tr.data :td])
        ;; TODO: do this the enlive way!
        (html/transform [:td] (html/clone-for [[y h] y-hs] [:td]  (fn [x] {:tag :td :content [(str y)] :attrs {:class (name h)}}))))))

(defn table
  ([data headers]
     (if (pos? (count headers))
      (let [values  (->> data (map (apply juxt headers)))]
         (-> data-snippet
             (html/transform [:tr.header :th] (html/clone-for [x headers] [:th] (html/content (name x))))
             (html/transform [:tr.data] (html/clone-for [x values] [:tr] (html/content (data-row x headers))))))))
  ([data]
     (table data (->> data (map keys) flatten distinct))))

(defn transpose
  [m]
  (->> m
       (into [])
       (map (fn [[k v]] {:field k :value v}))))

(defn html-table
  [data]
  (html/emit* (table data)))
