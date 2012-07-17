(ns roomba.core
  (:require [roomba.room :as room]))

(def ^:const number-of-moves 100)
(def ^:const number-of-sessions 200)

(defn run-cleaning-session
  "Returns the score for the strategy after moving the roomba number-of-moves"
  [strategy]
  (loop [world {:room (room/generate-room) :roomba (room/generate-roomba strategy) :score 0} move 1]
    (if (> move number-of-moves)
      (:score world)
      (recur (room/clean-step world) (inc move)))))

(defn generate-genome
  []
  (into []
        (repeatedly 243 #(rand-int 7))))

(defn generate-individual
  ([] (generate-individual (generate-genome)))
  ([genome]
     {:fitness 0 :genome genome}))

(defn calc-fitness
  [strategy]
  (let [totals (repeatedly number-of-sessions #(run-cleaning-session strategy))
        total-sum (reduce + totals)]
    (double (/ total-sum number-of-sessions))))

(defn calc-population-fitness
  [population]
  (vec (pmap #(assoc % :fitness (calc-fitness (:genome %)))
             population)))

(def ^:const number-of-generations 1000)
(def ^:const population-size 200)
(def ^:const tournament-size 5)

(defn most-fit
  [population]
  (first (sort-by :fitness > population)))

(defn tournament-selection
  "Randomly selects tournament-size individuals and chooses the most fit for mating."
  [population]
  (let [tourney-pop (repeatedly tournament-size #(rand-nth population))]
    (most-fit tourney-pop)))

(defn generate-offspring
  "Returns a pair of offspring by crossing over the genomes of mother and father"
  [[{mother :genome} {father :genome}]]
  (let [crossover-pt (rand-int 243)
        children [(vec (concat (subvec mother 0 crossover-pt) (subvec father crossover-pt)))
                  (vec (concat (subvec father 0 crossover-pt) (subvec mother crossover-pt)))]]
    (map generate-individual children)))

(defn next-generation
  [population]
  (->> (repeatedly population-size #(tournament-selection population))
       (partition 2)
       (mapcat generate-offspring)
       vec))

(defn evolve!
  []
  (let [initial-pop (vec (repeatedly population-size generate-individual))]
    (loop [population initial-pop generation 1]
      (if (> generation number-of-generations)
        (most-fit population)
        (let [fitness-pop (calc-population-fitness population)]
          (println generation (:fitness (most-fit fitness-pop)))
          (recur (next-generation fitness-pop)
                 (inc generation)))))))

(defn -main
  [& args]
  (evolve!))
