(ns back-office.scheduled
  (:use     [clojure.pprint])
  (:require [back-office.statements :as st]
            [back-office.transaction :as tr]
            [overtone.at-at :as at]
            [clojure.string :as string]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [entomic.api :as a]
            [back-office.database :as db])
  (:import [java.io File]))

(def default-dir "./docs")

(def archive-dir "./docs/archive")

(def processed-dir "./docs/archive/processed")

(def failed-dir "./docs/archive/failed")

(defn directory-of
  [file]
  (first (string/split (.getPath file) (re-pattern (.getName file)))))

(defn archive-file!
  [file success?]
  (let [dir (directory-of file)
        new (if success?
              (File. (str processed-dir "/" (.getName file)))
              (File. (str failed-dir "/" (.getName file))))]
    (io/copy file new)
    (io/delete-file file)))

(defn sub-dir?
  [dir file]
  (let [re (-> dir
               File.
               .getPath
               (str ".*")
               (string/replace #"\\" "#")
               re-pattern)
        s (-> (directory-of file)
              File.
              .getPath
              (string/replace #"\\" "#"))]
    (boolean (re-matches re s))))

(defn process-file?
  [file]
  (and
   (not (.isDirectory file))
   (not (sub-dir? archive-dir file))))

(defn process-file!
  [file]
  (if (process-file? file)
    (let [success? (try (st/save-statements! file)
                        (catch Exception e nil))]
      (archive-file! file success?))))

(defn process-files!
  ([dir]
     (map process-file! (file-seq (File. dir))))
  ([]
     (process-files! default-dir)))

(def pool (at/mk-pool))

(def repl-out *out*)

(def directory-summary
  (atom
   [{:file ""
     :status ""}]))

(defn all-files []
  (file-seq (File. "./docs")))

(defn failed-summary
  [files]
  (->> files
       (filter (partial sub-dir? failed-dir))
       (map (fn [f] {:file (str f) :status "failed"}))))

(defn process-statements! []
  (let [*out* repl-out
        files (all-files)
        new-files (filter process-file? files)
        queued-summary (map (fn [f] {:file (str f) :status "queued"}) (rest new-files))
        processed-summary (->> files
                               (filter (partial sub-dir? processed-dir))
                               (map (fn [f] {:file (str f) :status "processed"})))
        new-file (first new-files)]
    (reset! directory-summary
     (flatten
       [(if new-file
          [{:file (str new-file) :status "processing"}]
          [])
        queued-summary
        processed-summary
        (failed-summary files)]))
    (if new-file (process-file! new-file))))

(defn scheduled-jobs! []
  (process-statements!)
  (tr/process-all-transactions!))

(defn initialise-scheduled-jobs! []
  (db/init-db!)
  (at/every 1000 scheduled-jobs! pool :fixed-delay true))
