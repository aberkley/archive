(ns utilities.core
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as ce]))

(defn merge-date-and-time
  [date zone time]
  (t/from-time-zone (t/date-time (t/year date) (t/month date) (t/day date) (t/hour time) (t/minute time)) zone))

(defn date-dec
  ;extend this to cope with +ve offsets, just for generality
  ([d]
     (cond
      (= (t/day-of-week d) 1) (t/minus d (t/days 3))
      (= (t/day-of-week d) 7) (t/minus d (t/days 2))
      :else                   (t/minus d (t/days 1))))
  ([d i]
     (cond
      (< i 1) d
      :else (date-dec (date-dec d) (- i 1)))))

(defn date-inc
  ;extend this to cope with +ve offsets, just for generality
  ([d]
     (cond
      (= (t/day-of-week d) 5) (t/plus d (t/days 3))
      (= (t/day-of-week d) 6) (t/plus d (t/days 2))
      :else                   (t/plus d (t/days 1))))
  ([d i]
     (cond
      (< i 1) d
      :else (date-inc (date-inc d) (- i 1)))))

(defn date-adj
  [d i]
  (cond
   (> i 0) (date-inc d i)
   (= i 0) d
   (< i 0) (date-dec d (- 0 i))))

(defn working-days
  [start end]
  (let [e    (date-inc end)
        date (atom start)
        dates (atom [])]
    (while (t/before? @date e)
      (do
        (swap! dates conj @date)
        (swap! date date-inc)))
    @dates))

(defn- format-
  [formatter date]
  (->> date
       (ce/to-date-time)
       (f/unparse (f/formatter formatter))))

(def format-bb-date (partial format- "yyyyMMdd"))

(def format-bb-date-time (partial f/unparse (f/formatters :date-hour-minute-second)))

(def format-date (partial format- "yyyy-MM-dd"))

(def format-date-time (partial f/unparse (f/formatter "yyyy-MM-dd HH:mm")))

(defn- parse-or-nil
  [s formatter]
  (try (f/parse formatter s)
       (catch Exception e nil)))

(defn parse-instant
  [s]
  (let [custom-fs [(f/formatter "dd-MMM-yyyy")
                   (f/formatter "dd/MM/yyyy HH:mm:ss")
                   (f/formatter "dd/MM/yyyy HH:mm")]
        fs (->> (apply merge [:date :mysql] (keys f/formatters))
                (map f/formatters)
                (apply (partial merge custom-fs)))]
   (reduce (fn [t f] (if t t (parse-or-nil s f))) nil fs)))

(defn date-time?
  [i]
  (= (class i) org.joda.time.DateTime))

(defn unparse-instant
  [i]
  (if (date-time? i)
    (format-date-time i)
    (format-date i)))

(defn bigdec= [x y]
  (and
   (not (nil? x))
   (not (nil? y))
   (= 0 (.compareTo x y))))
