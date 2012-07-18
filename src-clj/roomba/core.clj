(ns roomba.core
  [require [roomba.genetic-algo :as algo]])

(defn -main
  [& args]
  (algo/evolve!))