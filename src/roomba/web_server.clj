(ns roomba.web-server
  (:use compojure.core)
  (:require [hiccup.page :as page]
            [hiccup.element :as elem]
            [compojure.route :as route]
            [compojure.handler :as handler]))

(defn- run-clojurescript [path init]
  (list
    (elem/javascript-tag "var CLOSURE_NO_DEPS = true;")
    (page/include-js path)
    (elem/javascript-tag init)))

(defn roomba-animation-page
  []
  (page/html5
   [:head
    [:title "Roomba!"]]
   [:body
    [:h1 "Roomba Time"]
    [:canvas#roomba-canvas {:width 500 :height 500}]
    (run-clojurescript "/js/roomba.js"
                       "roomba.animation.foo()")]))

(defroutes roomba-routes
  (GET "/"
       []
       (roomba-animation-page))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (handler/site roomba-routes))