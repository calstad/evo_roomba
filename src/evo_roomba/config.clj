(ns evo-roomba.config)

;; Genetic Algorithm configs
(def ^:const number-of-moves 100)
(def ^:const number-of-sessions 200)
(def ^:const number-of-generations 1000)
(def ^:const population-size 200)
(def ^:const tournament-size 5)

(def ^:const wall-penalty -5)
(def ^:const hairball-penalty -1)
(def ^:const hairball-reward 10)


;; Room configs
(def ^:const room-dimension 10)


;; Client side configs
(def fittest-uri
  "http://localhost:3000/fittest-individual")