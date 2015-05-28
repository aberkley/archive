(defproject sra-london/back-office "0.1.0-SNAPSHOT"
  :description "SRA Back Office"
  :url "https://github.com/sra-london/back-office"
  :min-lein-version "2.0.0"
  :dependencies [[enlive "1.1.5"]
                 [org.clojure/clojure "1.6.0"]
                 [compojure "1.1.9"]
                 [clj-time "0.6.0"]
                 [clj-http "0.7.8"]
                 [org.apache.pdfbox/pdfbox "1.8.4"]
                 [com.datomic/datomic-free "0.9.4815"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojars.aberkley/entomic "0.1.12-SNAPSHOT"]
                 [org.clojars.aberkley/utilities "0.1.3-SNAPSHOT"]
                 [org.clojars.aberkley/db "0.1.1-SNAPSHOT"]
                 [tulos/bberg-sdk "3.6.1.0"]
                 [overtone/at-at "1.2.0"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler back-office.handler/app
         :port 3000
         :init back-office.scheduled/initialise-scheduled-jobs!}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}
  :jvm-opts ["-Xmx1268M"]
  :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}
  :repositories [["releases" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/releases/"
                              :username :env
                              :password :env }]
                 ["snapshots" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/snapshots/"
                              :username :env
                              :password :env }]])
