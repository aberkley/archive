(defproject sra-london/benefix "0.1.0-SNAPSHOT"
  :description "A clojure DSL for the deconstruction and transformation of FIX messages"
  :url "https://github.com/sra-london/benefix"
  :license {:name "Eclipse Public License - v 1.0"}
  :dependencies [[org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.apache.mina/mina-core "1.1.7"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.quickfixj/quickfixj-all "1.5.3"]
                 [com.stuartsierra/component "0.2.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.apache.storm/storm-core "0.9.3"]
                 [yieldbot/marceline "0.2.1"]]
  :main benefix.core
  :aot :all
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}
  :repositories [["releases" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/releases/"
                              :username :env
                              :password :env }]
                 ["snapshots" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/snapshots/"
                              :username :env
                              :password :env}]])
