(defproject molecule "0.1.1-SNAPSHOT"
  :description "A datomic wrapper to make queries easier"
  :url "https://www.github.com/petergarbers/molecule"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot molecule.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[com.datomic/datomic-free "0.9.5561" :exclusions [joda-time]]]}})
