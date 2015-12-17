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
           was-authenticated (fn [session] (fn [user]
                                            (-> (redirect "/")
                                                (assoc :session (assoc session :identity user)))))
           was-invalidated (fn [session] (fn [& _]
                                          (-> (redirect "/")
                                              (assoc :session (dissoc session :identity)))))}}]
   (context base-path []
            (GET "/" [] (account-page {} {}))
            (POST "/login" [:as {session :session}]
                  (users/login user-store (was-authenticated session)
                               #(account-page % {})))
            (POST "/register" [:as {session :session}]
                  (users/register user-store email-service confirmation-email
                                  (was-authenticated session)
                                  #(account-page {} %)))
            (GET "/confirm/:token" [token :as {session :session}]
                 (users/confirm user-store (was-authenticated session)
                                confirm-failed-page))
            (GET "/reset-password" [] (request-reset-page {}))
            (POST "/reset-password" []
                  (users/request-reset user-store email-service reset-password-email
                                       #(request-reset-success-page %)
                                       #(request-reset-page %)))
            (GET "/reset-password/:token" [token]
                 (if (users/valid-reset-token? user-store token)
                   (reset-password-page token {})
                   (reset-password-bad-token-page)))
            (POST "/reset-password/:token" [token :as {session :session}]
                 (users/reset-password user-store
                                       (was-authenticated session)
                                       reset-password-bad-token-page
                                       #(reset-password-page token %)))
            (GET "/logout" [:as {session :session}]
                 (was-invalidated session)))))
