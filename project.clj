(defproject evo-roomba "0.1.0-SNAPSHOT"
  :description "Playing around with clojure reducers to parallelize computation."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main ^:skip-aot evo-roomba.core
  :warn-on-reflection true
  :plugins [[lein-cljsbuild "0.2.4"]
            [lein-ring "0.7.1"]]
  :hooks [leiningen.cljsbuild]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [hiccup "1.0.0"]
                 [compojure "1.1.1"]]
  :cljsbuild {
              :builds [{:source-path "src-cljs"
                        :compiler {:output-to "resources/public/js/roomba.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :crossovers [evo-roomba.cleaning-session evo-roomba.math evo-roomba.config]
              :crossover-path "crossover-cljs"}
  :ring {:handler evo-roomba.web-server/app})
