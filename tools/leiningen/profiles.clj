{:user {:plugins [[cider/cider-nrepl "0.7.0"]
                  [lein-ancient "0.5.5" :exclusions [org.clojure/clojure]]
                  [lein-kibit "0.0.8" :exclusions [org.clojure/clojure]]
                  [jonase/eastwood "0.1.4" :exclusions [org.clojure/clojure]]
                  [lein-cloverage "1.0.2"]
                  [lein-marginalia "0.8.0"]]
        :dependencies [[slamhound "1.5.5"]]
        :aliases {"slamhound" ["run" "-m" "slam.hound"]}
        :mirrors {#".+" {:name "nexus", :url "http://ec2-54-186-140-249.us-west-2.compute.amazonaws.com:8081/nexus/content/groups/public/"}}}}
