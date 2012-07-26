(ns roomba.animation
  (:require [clojure.browser.dom :as dom]
            [roomba.room :as room]
            [roomba.config :as config]))

(def canvas (dom/get-element "roomba-canvas"))

(def canvas-dim (.-width canvas))

(def cell-dim
  (/ canvas-dim config/room-dimension))

(defn canvas-ctx
  [canvas type]
  (.getContext canvas type))

(defn clear-ctx
  [ctx]
  (. ctx (clearRect 0 0 canvas-dim canvas-dim))
  ctx)

(defn scaled-coord
  [n]
  (* n cell-dim))

(defn scaled-coords
  [coords]
  (vec (map scaled-coord coords)))

(def cell-coords
  (map scaled-coord (range config/room-dimension)))

(defn cell-middle
  [coords]
  (let [[x y] coords
        mid-length (/ cell-dim 2)]
    [(+ x mid-length) (+ y mid-length)]))

(defn blank-rect
  [ctx [x y] w h]
  (. ctx (strokeRect x y w h))
  ctx)

(defn fill-rect
  [ctx [x y] w h]
  (. ctx (fillRect x y w h))
  ctx)

(defn fill-style
  [ctx color]
  (set! (.-fillStyle ctx) color)
  ctx)

(defn circle
  [ctx [x y] r]
  (. ctx (arc x y r 0 (* 2 Math.PI)))
  ctx)

(defn dirty-cell
  [ctx coords]
  (-> ctx
      (fill-style "#7B5B2D")
      (fill-rect coords cell-dim cell-dim)))

(defn clean-cell
  [ctx coords]
  (blank-rect ctx coords cell-dim cell-dim))

(defn draw-room
  [ctx room]
  (doseq [x (range config/room-dimension) y (range config/room-dimension)]
    (let [coords [x y]
          scaled-coords (scaled-coords coords)]
      (if (room/has-hairball? room coords)
        (dirty-cell ctx scaled-coords)
        (clean-cell ctx scaled-coords))))
  ctx)

(defn draw-roomba
  [ctx roomba]
  (let [coords (:pos roomba)
        roomba-center (-> coords scaled-coords cell-middle)]
    (-> ctx
        (fill-style "black")
        (circle roomba-center 10)
        (.fill)))
  ctx)

(defn draw
  [{:keys [room roomba]}]
  (let [ctx (canvas-ctx canvas "2d")]
    (-> ctx
        clear-ctx
        (draw-room room)
        (draw-roomba roomba))))