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

(defn- request-reset-page [data]
  (page (forms/build "/account/reset-password" schemata/request-reset
                     (merge data {:error-text-fn (fn [_ _ error]
                                                   (get users/text error (str error)))}))))

(defn- request-reset-success-page [user]
  (page [:p (format "%s we have sent you an email to %s. Please access your email and follow the instructions."
                    (:name user) (:email-address user))]
        [:p [:a {:href "/"} "Go back home."]]))

(defn- reset-password-page [token data]
  (page (forms/build (format "/account/reset-password/%s" token) schemata/reset-password
                     (merge data {:error-text-fn (fn [_ _ error]
                                                   (get users/text error (str error)))}))))

(defn- reset-password-bad-token-page []
  {:status 403
   :body (page [:p "You seem to be trying to reset your password, but your chance to do so has expired."])})

(defn- email-text-to-html [email]
  (let [text (-> email :message :text)
        [full-string path] (re-find #"(?m)^https?://[^/]+(/.*)$" text)]
    (clojure.string/replace text full-string (format "<a href=\"%s\">%s</a>" path full-string))))

(defn route-fn [{:keys [user-store email-service session-store]} _]
  (-> (routes

       (GET "/" [:as request]
            (page [:p "You are " [:span.status (if (authenticated? request) "Authenticated" "Unauthenticated")] "."]
                  [:p
                   [:a {:href "/account"} "Login/register"] ", "
                   [:a {:href "/account/reset-password"} "reset your password"] " or "
                   [:a {:href "/account/logout"} "logout"] "."]
                  [:p [:a {:href "/emails"} "Check emails sent."]]))

       (GET "/emails" []
            (page [:table
                   [:thead [:tr [:th "To"] [:th "Subject"] [:th "Message"]]]
                   [:tbody (for [email (-> email-service list-emails)]
                             [:tr [:td (:to email)] [:td (:subject email)] [:td (email-text-to-html email)]])]]))

       (user-routes/mount "/account" user-store email-service
                          {:account-page account-page
                           :confirm-failed-page (fn [] (page [:p "Invalid token used to confirm."]))
                           :request-reset-page request-reset-page
                           :request-reset-success-page request-reset-success-page
                           :reset-password-page reset-password-page
                           :reset-password-bad-token-page reset-password-bad-token-page})

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

