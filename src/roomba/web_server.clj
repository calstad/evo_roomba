(ns roomba.web-server
  (:use compojure.core
        [ring.util.response :only (response content-type)])
  (:require [roomba.genetic-algo :as algo]
            [hiccup.page :as page]
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
    [:h1 "Roomba Time!"]
    [:canvas#roomba-canvas {:width 500 :height 500}]
    (run-clojurescript "/js/roomba.js"
                       "roomba.core.run_evolution()")]))

(def evolution-future (atom nil))

(defn start-evolution
  []
  (if (nil? @evolution-future)
    (reset! evolution-future (future-call algo/evolve!))))

(defn stop-evolution
  []
  (if (not (future-done? @evolution-future))
    (do
      (future-cancel @evolution-future)
      (reset! evolution-future nil))))

(defroutes roomba-routes
  (GET "/"
       []
       (roomba-animation-page))
  (GET "/fittest-individual"
       []
       (content-type (response (with-out-str (prn fi))) "application/clojure"))
  (GET "/stop-evolution"
       []
       (stop-evolution)
       (response "EVOLUTION STOPPED!"))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (handler/site roomba-routes))