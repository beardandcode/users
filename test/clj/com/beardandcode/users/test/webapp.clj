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
            [com.beardandcode.users.routes :as user-routes]
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

(defn account-page [login-data register-data]
  (page (list (forms/build "/account/login" schemata/login
                           (merge login-data {:error-text-fn (fn [_ _ error]
                                                               (get users/text error (str error)))}))
              (forms/build "/account/register" schemata/register
                           (merge register-data {:error-text-fn (fn [_ _ error]
                                                                  (get users/text error (str error)))})))))

(defn reset-password-page [data]
  (page (forms/build "/account/reset-password" schemata/forgotten-password
                     (merge data {:error-text-fn (fn [_ _ error] (get users/text error (str error)))}))))

(defn route-fn [& _]
  (let [user-store (new-mem-store [["admin@user.com" "password" "Mr Admin"]])
        email-service (MemEmail. (atom []))]
    (-> (routes

         (GET "/" [:as request]
              (page [:div
                     [:p (str "Authenticated? " (authenticated? request))]
                     [:p
                      [:a {:href "/account"} "Login"] ", "
                      [:a {:href "/account"} "register"] ", "
                      [:a {:href "/account/reset-password"} "reset password"] " or "
                      [:a {:href "/account/logout"} "logout"] "."]
                     [:p "To see emails that have been sent, "
                      [:a {:href "/emails"} "go to the sent email list"] "."]]))

         (user-routes/mount "/account" user-store email-service
                            {:account-page account-page
                             :reset-password-page reset-password-page})

         (GET "/emails" []
              (page [:table
                     [:thead [:tr [:th "To"] [:th "Subject"] [:th "Body"] [:th]]]
                     [:tbody (map (fn [[to subject body]]
                                    [:tr [:td to] [:td subject] [:td body]
                                     [:td [:a {:href (str (if (.startsWith subject "Forgot")
                                                            "/account/reset-password/"
                                                            "/account/confirm/") body)} "Action!"]]])
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

