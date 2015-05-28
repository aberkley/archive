(defproject org.clojars.aberkley/db "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.6.0"]
                 [org.clojars.aberkley/utilities "0.1.3-SNAPSHOT"]
                 [org.clojars.aberkley/entomic "0.1.6-SNAPSHOT" :scope "test"]
                 [com.datomic/datomic-free "0.9.4880.2" :scope "test"]]
  :jvm-opts ["-Xmx768M"]
  :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}
  :repositories [["releases" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/releases/"
                              :username :env
                              :password :env }]
                 ["snapshots" {:url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/repositories/snapshots/"
                               :username :env
                               :password :env }]])
