(defproject roomba "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main roomba.core
  :warn-on-reflection true
  :plugins [[lein-cljsbuild "0.2.4"]]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/math.combinatorics "0.0.3"]]
  :cljsbuild {
              :builds [{:source-path "src-cljs"
                        :compiler {:output-to "resources/public/js/roomba.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :crossovers [roomba.room]
              :crossover-path "crossover-cljs"}
  :source-paths ["src-clj"])
