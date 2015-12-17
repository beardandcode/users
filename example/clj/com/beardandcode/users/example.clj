(ns com.beardandcode.users.example
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

(defn- page [request & body]
  (hiccup/html5
   [:head
    [:title "Example users webapp"]
    [:link {:rel "stylesheet" :type "text/css" :href "/static/normalize.css"}]
    [:link {:rel "stylesheet" :type "text/css" :href "/static/skeleton.css"}]
    [:link {:rel "stylesheet" :type "text/css" :href "/static/branding.css"}]]
   [:body
    [:div.container
     [:header.row
      [:a.logo.one-half.column {:href "/"} "Super Startup"]
      [:nav.one-half.column
       [:ul
        [:li [:a {:href "/emails"} "List emails sent by us"]]
        [:li.status (if (authenticated? request)
                      [:a {:href "/account/logout"} "Logout"]
                      [:a {:href "/account/"} "Login / register"])]]]]
     [:main.row body]]]))

(defn- account-page [request login-data register-data]
  (page request
        [:div#login.one-half.column
         [:h2 "Login"]
         (forms/build "/account/login" schemata/login
                      (merge login-data {:error-text-fn (fn [_ _ error]
                                                          (get users/text error (str error)))}))
         [:p "If you've forgotten your password, "
          [:a {:href "/account/reset-password"} "click here to reset your password"] "."]]
        [:div#register.one-half.column
         [:h2 "Register"]
         (forms/build "/account/register" schemata/register
                      (merge register-data {:error-text-fn (fn [_ _ error]
                                                             (get users/text error (str error)))}))]))

(defn- request-reset-page [request data]
  (page request
        [:h2 "Reset your password"]
        [:p "If you've forgotten your password you can reset it here. After you've entered your email address "
         "add hit submit we will send you an email with further instructions."]
        (forms/build "/account/reset-password" schemata/request-reset
                     (merge data {:error-text-fn (fn [_ _ error]
                                                   (get users/text error (str error)))}))))

(defn- request-reset-success-page [request user]
  (page request
        [:p (format "%s we have sent you an email to %s. Please access your email and follow the instructions."
                    (:name user) (:email-address user))]))

(defn- reset-password-page [request token data]
  (page request
        [:h2 "Reset your password"]
        [:p "Thanks for following the instructions in the email we sent you. You can now pick what your new "
         "password will be."]
        (forms/build (format "/account/reset-password/%s" token) schemata/reset-password
                     (merge data {:error-text-fn (fn [_ _ error]
                                                   (get users/text error (str error)))}))))

(defn- reset-password-bad-token-page [request]
  {:status 403
   :body (page request [:p "You seem to be trying to reset your password, but your chance to do so has expired."])})

(defn- email-text-to-html [email]
  (let [text (-> email :message :text)
        [full-string path] (re-find #"(?m)^https?://[^/]+(/.*)$" text)]
    (clojure.string/replace text full-string (format "<a href=\"%s\">%s</a>" path full-string))))

(defn route-fn [{:keys [user-store email-service session-store]} _]
  (-> (routes

       (GET "/" [:as request]
            (page request
                  [:h2 "Welcome to our super startup!"]
                  [:p
                   "This is a web application built to demonstrate the functionality of "
                   [:a {:href "https://github.com/beardandcode/users"} "com.beardandcode/users"] "."]
                  [:p
                   "You can register, login, confirm your account, reset your password and logout. "
                   "If you need to get to any emails sent, you can find a link at the top of the page."]))

       (GET "/emails" [:as request]
            (page request
                  [:table
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

