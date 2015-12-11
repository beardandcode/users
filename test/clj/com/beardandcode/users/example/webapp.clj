(ns com.beardandcode.users.example.webapp
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [com.beardandcode.components.email.mock :refer [new-mock-email-service list-emails]]
            [com.beardandcode.components.routes :refer [new-routes]]
            [com.beardandcode.components.session.mock :refer [new-mock-session-store]]
            [com.beardandcode.components.web-server :refer [new-web-server]]
            [com.beardandcode.forms :as forms]
            [com.beardandcode.users :as users]
            [com.beardandcode.users.routes :as user-routes]
            [com.beardandcode.users.schemata :as schemata]
            [com.beardandcode.users.store :refer [new-mem-store]]))

(defn- page [& body]
  (hiccup/html5
   [:head
    [:title "Test webapp"]
    [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
   [:body body]))

(defn- account-page [login-data register-data]
  (page [:div#login (forms/build "/account/login" schemata/login
                                 (merge login-data {:error-text-fn (fn [_ _ error]
                                                                     (get users/text error (str error)))}))]
        [:div#register (forms/build "/account/register" schemata/register
                                    (merge register-data {:error-text-fn (fn [_ _ error]
                                                                           (get users/text error (str error)))}))]))

(defn- email-text-to-html [email]
  (let [text (-> email :message :text)
        [full-string path] (re-find #"(?m)^https?://[^/]+(/.*)$" text)]
    (clojure.string/replace text full-string (format "<a href=\"%s\">%s</a>" path full-string))))

(defn route-fn [{:keys [user-store email-service session-store]} _]
  (-> (routes

       (GET "/" [:as request]
            (page [:p "You are " [:span.status (if (authenticated? request) "Authenticated" "Unauthenticated")] "."]
                  [:p
                   [:a {:href "/account"} "Login/register"] " or "
                   [:a {:href "/account/logout"} "logout"] "."]
                  [:p [:a {:href "/emails"} "Check emails sent."]]))

       (GET "/emails" []
            (page [:table
                   [:thead [:tr [:th "To"] [:th "Subject"] [:th "Message"]]]
                   [:tbody (for [email (-> email-service list-emails)]
                             [:tr [:td (:to email)] [:td (:subject email)] [:td (email-text-to-html email)]])]]))

       (user-routes/mount "/account" user-store email-service
                          {:account-page account-page
                           :confirm-failed-page (fn [] (page [:p "Invalid token used to confirm."]))})

       (route/resources "/static/"))

      (wrap-authentication (session-backend))
      (wrap-session {:store session-store})
      wrap-params))


(defn new-test-system [port]
  (component/system-map
   :session-store (new-mock-session-store)
   :email-service (new-mock-email-service)
   :user-store (new-mem-store)
   :routes (component/using
            (new-routes route-fn)
            [:user-store :email-service :session-store])
   :web (component/using
         (new-web-server "127.0.0.1" port)
         [:routes])))

