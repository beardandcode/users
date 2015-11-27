(ns com.beardandcode.users.test.webapp
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [redirect]]
            [com.beardandcode.components.email :refer [IEmail]]
            [com.beardandcode.components.routes :refer [new-routes]]
            [com.beardandcode.components.web-server :refer [new-web-server]]
            [com.beardandcode.forms :as forms]
            [com.beardandcode.users :as users]
            [com.beardandcode.users.schemata :as schemata]
            [com.beardandcode.users.store :refer [new-mem-store]]))

(defn page [body]
  (hiccup/html5
   [:head
    [:title "Test webapp"]
    [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
   [:body body]))

(defn login-page
  ([] (login-page {}))
  ([data]
   (page (forms/build "/login" schemata/login
                      (merge data {:error-text-fn (fn [_ _ error] (get users/text error (str error)))})))))

(defn register-page
  ([] (register-page {}))
  ([data]
   (page (forms/build "/register" schemata/register
                      (merge data {:error-text-fn (fn [_ _ error] (get users/text error (str error)))})))))

(defn route-fn [& _]
  (let [user-store (new-mem-store [["admin@user.com" "password" "Mr Admin"]])]
    (-> (routes

         (GET "/" [:as request]
              (page [:div
                     [:p (str "Authenticated? " (authenticated? request))]
                     [:p
                      [:a {:href "/login"} "Login"] ", "
                      [:a {:href "/register"} "register"] " or "
                      [:a {:href "/logout"} "logout"] "."]]))

         (GET "/login" [] (login-page))

         (POST "/login" [:as {session :session}]
               (users/login user-store
                            #(-> (redirect "/")
                                 (assoc :session (assoc session :identity %1)))
                            #(login-page %1)))

         (GET "/register" [] (register-page))

         (POST "/register" [:as {session :session}]
               (users/register user-store
                               #(-> (redirect "/")
                                    (assoc :session (assoc session :identity %1)))
                               #(register-page %1)))

         (GET "/logout" [:as {session :session}]
              (-> (redirect "/")
                  (assoc :session (dissoc session :identity))))

         (route/resources "/static/"))

        (wrap-authentication (session-backend))
        wrap-session
        wrap-params)))


(defn new-test-system [port]
  (component/system-map
   :routes (new-routes route-fn)
   :web (component/using
         (new-web-server "127.0.0.1" port)
         [:routes])))

