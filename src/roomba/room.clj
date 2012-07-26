(ns roomba.room
  (:require [roomba.math :as combo]
            [roomba.config :as config]))

(defn generate-room
  []
  (vec (repeatedly (* config/room-dimension config/room-dimension) #(rand-int 2))))

(defn coords->idx
  [[x y]]
  (+ x (* y config/room-dimension)))

(defn get-cell
  [room coords]
  (nth room (coords->idx coords)))

(defn wall?
  [[x y]]
  (let [adjusted-dim (dec config/room-dimension)]
    (or (> 0 x)
        (> 0 y)
        (> x adjusted-dim)
        (> y adjusted-dim))))

(defn has-hairball?
  [room coords]
  (= 1 (get-cell room coords)))

(def cell-states
  "Each cell in the room is one of these three states."
  [:empty :hairball :wall])

(defn get-cell-state
  "Tells whether the cell at coords in room is a wall, has a hairball in it, or empty."
  [room coords]
  (cond
   (wall? coords) :wall
   (has-hairball? room coords) :hairball
   :else :empty))

(def situation-map
  "A map where the keys are the states from the north, south, east, west, and current cell and the values are the index in the strategy for the corresponding neighborhood situation."
  (let [all-situations (combo/selections cell-states 5)]
    (into {}
          (map-indexed (fn [idx situation] [(vec situation) idx])
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
  (vec (for [dir dirs] (coords-in-dir current-pos dir))))

(defn current-situation
  "Returns the states of the cells directly to the north, south, east, west, and current position of the roomba."
  [room roomba]
  (let [current-pos (:pos roomba)]
    (vec (map #(get-cell-state room %) (neighborhood current-pos)))))

(defn move-roomba
  [roomba new-pos]
  (assoc roomba :pos new-pos))

(defn pickup-hairball
  [room current-pos]
  (assoc room (coords->idx current-pos) 0))

(def actions
  {0 {:type :move :dir :north}
   1 {:type :move :dir :south}
   2 {:type :move :dir :east}
   3 {:type :move :dir :west}
   4 {:type :move :dir :north}
   5 {:type :pickup}
   6 {:type :random}})

;; TODO Clean up returing of score.
(def ^:const wall-penalty -5)
(def ^:const hairball-penalty -1)
(def ^:const hairball-reward 10)

(defmulti exec-action
  (fn [world action]
    (:type action)))

(defmethod exec-action :move
  [{:keys [room roomba score]} action]
  (let [dir (:dir action)
        current-pos (:pos roomba)
        new-pos (coords-in-dir current-pos dir)]
    (if (not (wall? new-pos))
      {:room room :roomba (move-roomba roomba new-pos) :score score}
      {:room room :roomba roomba :score (+ score wall-penalty)})))

(defmethod exec-action :pickup
  [{:keys [room roomba score]} action]
  (let [current-pos (:pos roomba)]
    (if (has-hairball? room current-pos)
      {:room (pickup-hairball room current-pos) :roomba roomba :score (+ score hairball-reward)}
      {:room room :roomba roomba :score (+ score hairball-penalty)})))

(defmethod exec-action :random
  [world action]
  (exec-action world (get actions (rand-int 7))))

(defn next-action
  [room roomba]
  (let [situation (current-situation room roomba)
        situation-idx (get situation-map situation)
        action-key (nth (:strategy roomba) situation-idx)]
    (get actions action-key)))

(defn clean-step
  "Returns the world after taking the single action taken"
  [{:keys [room roomba] :as world}]
  (let [action (next-action room roomba)]
    (exec-action world action)))

(defn generate-roomba
  [strategy]
  {:pos [0 0] :strategy strategy})

