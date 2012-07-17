(ns roomba.core
  (:use [clojure.string :only (join)])
  (:require [clojure.math.combinatorics :as combo]))

;; Defining the room
(def ^:const room-dimension 10)

(defn generate-room
  []
  (vec (repeatedly (* room-dimension room-dimension) #(rand-int 2))))

(defn coords->idx
  [[x y]]
  (+ x (* y room-dimension)))

(defn get-cell
  [room coords]
  (nth room (coords->idx coords)))

(defn add-hairballs!
  "Places a hairball in a cell with a .5 probability."
  [room]
  (doseq [row room cell row]
    (if (= 1 (rand-int 2))
      (swap! cell assoc :hairball 1)))
  room)

(defn wall?
  [[x y]]
  (letfn [(out-of-bounds? [n]
            (or (> 0 n) (> n (dec room-dimension))))]
    (or (out-of-bounds? x)
        (out-of-bounds? y))))

(defn has-hairball?
  [room coords]
  (let [cell (get-cell room coords)]
    (= 1 (:hairball @cell))))

(def cell-states
  "Each cell in the room is one of these three states."
  ["empty" "hairball" "wall"])

(defn get-cell-state
  "Tells whether the cell at coords in room is a wall, has a hairball in it, or empty."
  [room coords]
  (cond
   (wall? coords) "wall"
   (has-hairball? room coords) "hairball"
   :else "empty"))

(defn hash-situation
  [situation]
  (hash (join situation)))

(def situation-map
  "A map where the keys are the hash of the string formed by joining the states from the north, south, east, west, and current cell and the values are the index in the strategy for the corresponding neiborhood situation."
  (let [all-situations (combo/selections cell-states 5)]
    (into {}
          (map-indexed (fn [idx situation] [(hash-situation situation) idx])
                       all-situations))))

(def dirs [:north :south :east :west :current])

(def dir-deltas
  {:north [0 1]
   :south [0 -1]
   :east [1 0]
   :west [-1 0]
   :current [0 0]})

(defn coords-in-dir
  "Returns the coordinates in direction of dir from the specified coordinates."
  [[x y] dir]
  (let [[dx dy] (dir dir-deltas)]
    [(+ x dx) (+ y dy)]))

(defn neighborhood
  "Returns the coordinates of the cells directly to the north, south, east, west, and current position."
  [current-pos]
  (vec (map (partial coords-in-dir current-pos) dirs)))

(defn current-situation
  "Returns the states of the cells directly to the north, south, east, west, and current position of the roomba."
  [room roomba]
  (let [current-pos (:pos @roomba)]
    (map (partial get-cell-state room) (neighborhood current-pos))))

(defn move-roomba
  "Moves the roomba unless the new position is a wall. Must be in a transaction."
  [roomba new-pos]
  (swap! roomba assoc :pos new-pos))

(defn pickup-hairball
  "Picks up a hairball if there is one at the roomba's current position.  Must be in a transaction."
  [room current-pos]
  (let [current-cell (get-cell room current-pos)]
    (swap! current-cell assoc :hairball 0)))

(def actions
  {0 {:type :move :dir :north}
   1 {:type :move :dir :south}
   2 {:type :move :dir :east}
   3 {:type :move :dir :west}
   4 {:type :move :dir :north}
   5 {:type :pickup}
   6 {:type :move :dir (rand-nth dirs)}})

;; TODO Clean up returing of score.
(def ^:const wall-penalty -5)
(def ^:const hairball-penalty -1)
(def ^:const hairball-reward 10)

(defmulti exec-action
  (fn [room roomba action]
    (:type action)))

(defmethod exec-action :move
  [room roomba action]
  (let [dir (:dir action)
        current-pos (:pos @roomba)
        new-pos (coords-in-dir current-pos dir)]
    (if (not (wall? new-pos))
      (do
        (move-roomba roomba new-pos)
        0)
      wall-penalty)))

(defmethod exec-action :pickup
  [room roomba action]
  (let [current-pos (:pos @roomba)]
    (if (has-hairball? room current-pos)
      (do
        (pickup-hairball room current-pos)
        hairball-reward)
      hairball-penalty)))

(defn next-action
  [room roomba]
  (let [situation (current-situation room roomba)
        situation-idx (get situation-map (hash-situation situation))
        action-key (nth (:strategy @roomba) situation-idx)]
    (get actions action-key)))

(defn clean-step
  "Returns the score for the single action taken"
  [room roomba]
  (let [action (next-action room roomba)]
    (exec-action room roomba action)))

(def ^:const number-of-moves 100)
(def ^:const number-of-sessions 200)

(defn generate-roomba
  [strategy]
  (atom {:pos [0 0] :strategy strategy}))

(defn run-cleaning-session
  [strategy]
  (let [room (add-hairballs! (generate-room))
        roomba (generate-roomba strategy)]
    (reduce + (repeatedly number-of-moves #(clean-step room roomba)))))

(defn generate-genome
  []
  (reduce (fn [genome _]
            (conj genome (rand-int 7)))
          []
          (range 243)))

(defn generate-individual
  [& genome]
  (let [g (if genome (vec (first genome)) (generate-genome))]
    {:fitness 0 :genome g}))

(defn calc-fitness
  [strategy]
  (let [totals (repeatedly number-of-sessions #(run-cleaning-session strategy))
        total-sum (reduce + totals)]
    (double (/ total-sum (count totals)))))

(defn calc-population-fitness
  [population]
  (doall
   (vec (pmap #(assoc % :fitness (calc-fitness (:genome %)))
              population))))

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
  [{mother :genome} {father :genome}]
  (let [crossover-pt (inc (rand-int 242))
        children [(concat (take crossover-pt mother) (drop crossover-pt father))
                  (concat (take crossover-pt father) (drop crossover-pt mother))]]
    (map generate-individual children)))

(defn next-generation
  [population]
  (flatten (repeatedly (/ population-size 2)
                       #(generate-offspring (tournament-selection population)
                                            (tournament-selection population)))))

(defn evolve!
  []
  (let [initial-pop (repeatedly population-size generate-individual)]
    (loop [population initial-pop generation 1]
      (if (> generation number-of-generations)
        (most-fit population)
        (let [fitness-pop (calc-population-fitness population)]
          (println (:fitness (most-fit fitness-pop)))
          (recur (next-generation fitness-pop)
                 (inc generation)))))))
