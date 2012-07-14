(ns roomba.core
  (:use [clojure.string :only (join)])
  (:require [clojure.math.combinatorics :as combo]))

(def room-dimension 10)

(def wall-penalty -5)
(def hairball-penalty -1)
(def hairball-reward 10)

(def number-of-moves 100)
(def number-of-sessions 200)

(def number-of-generations 1000)
(def population-size 200)

(defn generate-room
  "Generates a 2D vector with no hairballs in each cell."
  []
  (apply vector
         (map (fn [_]
                (apply vector (map (fn [_] (atom {:hairball 0}))
                                   (range room-dimension))))
              (range room-dimension))))

(defn generate-roomba
  [strategy]
  (atom {:pos [0 0] :strategy strategy}))

(defn get-cell
  "Returns the atom for the given coordinate in the room."
  [room [x y]]
  (-> room (nth y) (nth x)))

(defn add-hairballs!
  "Places a hairball in a cell with a .5 probability."
  [room]
  (doseq [row room cell row]
    (if (= 1 (rand-int 2))
      (swap! cell assoc :hairball 1)))
  room)

(defn hash-situation
  [situation]
  (hash (join situation)))

(def cell-states
  ["empty" "hairball" "wall"])

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

(defn get-cell-state
  "Tells whether the cell at coords in room is a wall, has a hairball in it, or empty."
  [room coords]
  (cond
   (wall? coords) "wall"
   (has-hairball? room coords) "hairball"
   :else "empty"))

(def situation-map
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
  []
  {:fitness 0 :genome (generate-genome)})

(defn calc-fitness
  [strategy]
  (let [totals (repeatedly number-of-sessions #(run-cleaning-session strategy))
        total-sum (reduce + totals)]
    (double (/ total-sum (count totals)))))

(defn calc-population-fitness
  [population]
  (doall (pmap #(calc-fitness (:genome %)) population)))