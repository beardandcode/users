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

(defprotocol IListEmails
  (list-emails [_]))

(defrecord MemEmail [emails]
  IEmail
  (send-email [_ to subject body] (swap! emails conj [to subject body]))

  IListEmails
  (list-emails [_] @emails))

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

(defn forgotten-password-page [data]
  (page (forms/build "/forgotten-password" schemata/forgotten-password
                     (merge data {:error-text-fn (fn [_ _ error] (get users/text error (str error)))}))))

(defn route-fn [& _]
  (let [user-store (new-mem-store [["admin@user.com" "password" "Mr Admin"]])
        email-service (MemEmail. (atom []))]
    (-> (routes

         (GET "/" [:as request]
              (page [:div
                     [:p (str "Authenticated? " (authenticated? request))]
                     [:p
                      [:a {:href "/login"} "Login"] ", "
                      [:a {:href "/register"} "register"] ", "
                      [:a {:href "/forgotten-password"} "forgotten password"] " or "
                      [:a {:href "/logout"} "logout"] "."]
                     [:p "To see emails that have been sent, "
                      [:a {:href "/emails"} "go to the sent email list"] "."]]))

         (GET "/login" [] (login-page))

         (POST "/login" [:as {session :session}]
               (users/login user-store
                            #(-> (redirect "/")
                                 (assoc :session (assoc session :identity %1)))
                            #(login-page %1)))

         (GET "/register" [] (register-page))

         (POST "/register" [:as {session :session}]
               (users/register user-store
                               email-service
                               #(-> (redirect "/")
                                    (assoc :session (assoc session :identity %1)))
                               #(register-page %1)))

         (GET "/confirm/:token" [:as {session :session}]
              (users/confirm user-store
                             #(-> (redirect "/")
                                  (assoc :session (assoc session :identity %1)))
                             #(redirect "/")))

         (GET "/forgotten-password" [email-address]
              (forgotten-password-page {:values {"email-address" email-address}}))

         (POST "/forgotten-password" []
               (users/confirm-forgotten-password user-store
                                                 email-service
                                                 #(redirect "/")
                                                 #(forgotten-password-page %)))

         (GET "/forgotten-password/:token" [:as {session :session}]
              (users/forgotten-password user-store
                                        #(-> (redirect "/")
                                             (assoc :session (assoc session :identity %1)))
                                        #(redirect "/")))

         (GET "/logout" [:as {session :session}]
              (-> (redirect "/")
                  (assoc :session (dissoc session :identity))))

         (GET "/emails" []
              (page [:table
                     [:thead [:tr [:th "To"] [:th "Subject"] [:th "Body"] [:th]]]
                     [:tbody (map (fn [[to subject body]]
                                    [:tr [:td to] [:td subject] [:td body]
                                     [:td [:a {:href (str (if (.startsWith subject "Forgot")
                                                            "/forgotten-password/"
                                                            "/confirm/") body)} "Action!"]]])
                                  (list-emails email-service))]]))

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

