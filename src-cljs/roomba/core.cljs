(ns roomba.core
  (:require [roomba.animation :as animation]
            [roomba.client :as client]
            [roomba.room :as room]
            [roomba.config :as config]
            [goog.dom :as dom]))

(defn run-animation
  [roomba]
  (let [genome (:strategy roomba)
        window (dom/getWindow)]
    (. window (setTimeout
               (fn []
                 (room/run-cleaning-session genome animation/draw))
               1000))))

(defn run-evolution
  []
  (client/get-fittest-roomba run-animation))