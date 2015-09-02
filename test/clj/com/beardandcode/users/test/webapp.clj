(ns com.beardandcode.users.test.webapp
  (:require [clojure.java.shell :as shell]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]))


(defn route-fn []
  (-> (routes

       (GET "/" [] (hiccup/html5
                    [:head
                     [:title "Test webapp"]
                     [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
                    [:body]))

       (route/resources "/static/"))))


(def server nil)

(defn webapp-port [] (when server (-> server .getConnectors first .getLocalPort)))

(defn start-webapp!
  ([]
     (start-webapp! 0)
     (println (str "Listening on http://localhost:" (webapp-port) "/")))
  ([port]
     (alter-var-root (var server)
                     (fn [server] (or server (run-jetty (route-fn) {:port port :join? false}))))))

(defn open-webapp! []
  (shell/sh "open" (str "http://localhost:" (webapp-port) "/")))

(defn stop-webapp! []
  (alter-var-root (var server) #(when % (.stop %))))

