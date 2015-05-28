(ns utilities.ns)

(defn intern-ns
  [ns-from ns-to]
  (let [ns-from' (if (symbol? ns-from) (find-ns ns-from) ns-from)
        ns-to' (if (symbol? ns-to) (find-ns ns-to) ns-to)]
    (doall
     (->> ns-from'
          ns-interns
          (map (fn [[s f]] (intern ns-to' s f)))))))
