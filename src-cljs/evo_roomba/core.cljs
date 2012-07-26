(ns evo-roomba.core
  (:require [evo-roomba.animation :as animation]
            [evo-roomba.client :as client]
            [evo-roomba.cleaning-session :as cs]
            [evo-roomba.config :as config]
            [goog.dom :as dom]))

(defn run-animation
  [roomba]
  (let [genome (:strategy roomba)
        window (dom/getWindow)]
    (. window (setTimeout
               (fn []
                 (cs/run-cleaning-session genome animation/draw))
               1000))))

(defn run-evolution
  []
  (client/get-fittest-roomba run-animation))