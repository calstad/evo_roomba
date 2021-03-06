(ns evo-roomba.genetic-algo
  (:require [evo-roomba.cleaning-session :as cs]
            [evo-roomba.config :as config]))

(def fittest-individual (atom nil))

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
  (let [totals (repeatedly config/number-of-sessions #(cs/run-cleaning-session strategy))
        total-sum (reduce + totals)]
    (double (/ total-sum config/number-of-sessions))))

(defn calc-population-fitness
  [population]
  (vec (pmap #(assoc % :fitness (calc-fitness (:genome %)))
             population)))

(defn most-fit
  [population]
  (first (sort-by :fitness > population)))

(defn tournament-selection
  "Randomly selects tournament-size individuals and chooses the most fit for mating."
  [population]
  (let [tourney-pop (repeatedly config/tournament-size #(rand-nth population))]
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
  (->> (repeatedly config/population-size #(tournament-selection population))
       (partition 2)
       (mapcat generate-offspring)
       vec))

(defn evolve!
  []
  (let [initial-pop (vec (repeatedly config/population-size generate-individual))]
    (loop [population initial-pop generation 1]
      (if (> generation config/number-of-generations)
        @fittest-individual
        (let [fitness-pop (calc-population-fitness population)
              fittest (most-fit fitness-pop)]
          (println generation (:fitness fittest))
          (reset! fittest-individual fittest)
          (recur (next-generation fitness-pop)
                 (inc generation)))))))

(defn -main
  [& args]
  (evolve!))
