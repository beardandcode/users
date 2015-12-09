(ns com.beardandcode.users.example.webapp
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
            [com.beardandcode.components.routes :refer [new-routes]]
            [com.beardandcode.components.web-server :refer [new-web-server]]
            [com.beardandcode.forms :as forms]
            [com.beardandcode.users.routes :as user-routes]
            [com.beardandcode.users.schemata :as schemata]))


(defn- page [body]
  (hiccup/html5
   [:head
    [:title "Test webapp"]
    [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
   [:body body]))

(defn- account-page []
  (page (forms/build "/account" schemata/login)))

(defn route-fn [& _]
  (-> (routes

       (GET "/" [] (page [:p [:a {:href "/account"} "Login"]]))

       (user-routes/mount "/account"
                          {:account-page account-page})

       (route/resources "/static/"))))


(defn new-test-system [port]
  (component/system-map
   :routes (new-routes route-fn)
   :web (component/using
         (new-web-server "127.0.0.1" port)
         [:routes])))

