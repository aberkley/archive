(defproject sra-london/data-feed "0.1.0-SNAPSHOT"
  :description "Data aggregator for static data for the back office project"
  :url "https://github.com/sra-london/data-feed"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.1"]
                 [ring/ring-core "1.3.2"]
                 [liberator "0.12.2"]
                 [org.clojars.aberkley/entomic "2.1.0-SNAPSHOT" :exclusions [org.jsoup/jsoup]]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.10"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [clj-http "1.0.1" :exclusions [org.clojure/tools.reader com.fasterxml.jackson.core/jackson-core]]]
  :plugins [[lein-ring "0.8.13" :exclusions [org.clojure/clojure]]]
  :ring {:handler data-feed.core/handler, :init data-feed.core/init}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [org.clojure/test.check "0.7.0"]]}}
  :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}
  :repositories [["releases" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/releases/"
                              :username :env
                              :password :env }]
                 ["snapshots" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/snapshots/"
                               :username :env
                               :password :env }]])
