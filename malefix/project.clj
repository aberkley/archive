(defproject sra-london/malefix "0.1.0-SNAPSHOT"
  :description "A clojure DSL for the deconstruction and transformation of FIX messages"
  :url "https://github.com/sra-london/malefix"
  :license {:name "Eclipse Public License - v 1.0"}
  :dependencies [[ch.qos.logback/logback-classic "1.1.2"]
                 [clj-kafka "0.2.8-0.8.1.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.apache.mina/mina-core "1.1.7"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.quickfixj/quickfixj-all "1.5.3"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :main malefix.core
  :aot [malefix.core]
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}
  :repositories [["releases" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/releases/"
                              :username :env
                              :password :env }]
                 ["snapshots" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/snapshots/"
                               :username :env
                               :password :env}]])
