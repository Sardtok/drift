;; IMPORTANT: When bumping the version number here, be sure to bump it also in
;; src/drift/drift_version.clj! 
(defproject com.sigmund-hansen/drift "2.0.0-RC3"
  :description "Drift 2 is a rails like migration framework for Clojure compatible with Java 8-21+."
  :url "https://github.com/Sardtok/drift"
  :license {:name         "Apache-2.0 License"
            :url          "https://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/tools.logging "1.2.4"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.11.1"]
                                  [log4j/log4j "1.2.17"]]}}

  :aot [drift.listener-protocol drift.Drift])
