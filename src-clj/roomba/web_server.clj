(ns roomba.web-server
  (:use compojure.core)
  (:require [hiccup.page :as page]
            [hiccup.element :as elem]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(defroutes roomba-routes
  (GET "/" [] "Roomba!")
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (handler/site roomba-routes))