(defproject sra-london/elsa "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.10"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [sra-london/clj-bb-api "0.2.0-SNAPSHOT"]
                 [org.clojars.aberkley/utilities "0.1.3-SNAPSHOT" :exclusions [org.jsoup/jsoup]]
                 [com.stuartsierra/component "0.2.2"]
                 [clojurewerkz/quartzite "2.0.0" :exclusions [clj-time joda-time]]
                 [org.clojure/test.check "0.7.0"]]
  :main elsa.core
  :aot [elsa.core]
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]
                        [sra-london/bloomberg-emulator "0.0.1"]]}}
  :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}
  :repositories [["releases" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/releases/"
                              :username :env
                              :password :env }]
                 ["snapshots" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/snapshots/"
                               :username :env
                               :password :env }]])
