(ns roomba.animation
  (:require [clojure.browser.dom :as dom]))

(def canvas (dom/get-element "roomba-canvas"))

(def room-dim 10)

(def canvas-dim (.getAttribute canvas "width"))

(defn canvas-ctx
  [canvas type]
  (.getContext canvas type))

(def ctx
  (canvas-ctx canvas "2d"))

(defn rect
  [ctx x y w h]
  (.strokeRect ctx x y w h))

(defn draw
  []
  (let [rect-dim (/ canvas-dim room-dim)]
    (doseq [x (range room-dim) y (range room-dim)]
      (rect ctx (* x rect-dim) (* y rect-dim) rect-dim rect-dim))))
