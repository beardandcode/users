(ns com.beardandcode.users.routes
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [com.beardandcode.users :as users]))

(defn- unimplemented-page [& _]
  "<h1>Unimplemented page</h1>")

(defn- basic-confirm-email [base-path]
  (fn [user confirmation-token]
    {:subject (format "Hi %s, confirm your email address to get started." (:email-address user))
     :message {:text (format "Hi %s,\n\nhttp://example.com%s/confirm/%s\n\nThanks,\nTeam"
                             (or (:name user) (:email-address user)) base-path confirmation-token)}}))

(defn- basic-reset-email [base-path]
  (fn [user confirmation-token]
    {:subject (format "Follow these instructions to change your password" (:email-address user))
     :message {:text (format "Hi %s,\n\nhttp://example.com%s/reset-password/%s\n\nThanks,\nTeam"
                             (or (:name user) (:email-address user)) base-path confirmation-token)}}))

(defn mount
  ([b u p e] (mount b u p e {}))
  ([base-path user-store email-service
    {:keys [account-page confirm-failed-page request-reset-page request-reset-success-page
            reset-password-page reset-password-bad-token-page]
      :or {account-page unimplemented-page
           confirm-failed-page unimplemented-page
           request-reset-page unimplemented-page
           request-reset-success-page unimplemented-page
           reset-password-page unimplemented-page
           reset-password-bad-token unimplemented-page}}
     {:keys [confirmation-email reset-password-email was-authenticated was-invalidated]
      :or {confirmation-email (basic-confirm-email base-path)
           reset-password-email (basic-reset-email base-path)
           was-authenticated (fn [request user]
                               (-> (redirect "/")
                                   (assoc :session (assoc (:session request) :identity user))))
           was-invalidated (fn [request & _]
                             (-> (redirect "/")
                                 (assoc :session (dissoc (:session request) :identity))))}}]
   (context base-path []
            (GET "/" [:as request] (account-page request {} {}))
            (POST "/login" []
                  (users/login user-store was-authenticated
                               #(account-page %1 %2 {})))
            (POST "/register" []
                  (users/register user-store email-service confirmation-email
                                  was-authenticated
                                  #(account-page %1 {} %2)))
            (GET "/confirm/:token" [token]
                 (users/confirm user-store was-authenticated
                                confirm-failed-page))
            (GET "/reset-password" [:as request] (request-reset-page request {}))
            (POST "/reset-password" []
                  (users/request-reset user-store email-service reset-password-email
                                       request-reset-success-page
                                       request-reset-page))
            (GET "/reset-password/:token" [token :as request]
                 (if (users/valid-reset-token? user-store token)
                   (reset-password-page request token {})
                   (reset-password-bad-token-page request)))
            (POST "/reset-password/:token" [token]
                 (users/reset-password user-store
                                       was-authenticated
                                       reset-password-bad-token-page
                                       #(reset-password-page %1 token %2)))
            (GET "/logout" []
                 was-invalidated))))
