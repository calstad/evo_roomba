(ns roomba.client
  (:require [roomba.config :as config]
            [cljs.reader :as reader]
            [goog.dom :as dom]
            [goog.net.XhrIo :as xhr]))

(declare get-fittest-roomba)

(defn resp->clj
  [resp]
  (-> resp/target
      .getResponseText
      reader/read-string))

(defn retry-request
  [callback]
  (let [window (dom/getWindow)
        retry-fn (fn [] (get-fittest-roomba callback))]
    (. window (setTimeout retry-fn 5000))))

(defn process-response
  [xhr-resp callback]
  (let [parsed-resp (resp->clj xhr-resp)]
    (if (nil? parsed-resp)
      (retry-request callback)
      (callback parsed-resp))))

(defn get-fittest-roomba
  [callback]
  (xhr/send config/fittest-uri (fn [resp] (process-response resp callback)) "GET"))

