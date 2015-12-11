(ns com.beardandcode.users.routes
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [com.beardandcode.users :as users]))

(defn- unimplemented-page [& _]
  "<h1>Unimplemented page</h1>")

(defn- basic-email [base-path]
  (fn [user confirmation-token]
    {:subject (format "Hi %s, confirm your email address to get started." (:email-address user))
     :message {:text (format "Hi %s,\n\nhttp://example.com%s/confirm/%s\n\nThanks,\nTeam"
                             (or (:name user) (:email-address user)) base-path confirmation-token)}}))

(defn mount
  ([b u p e] (mount b u p e {}))
  ([base-path user-store email-service
     {:keys [account-page confirm-failed-page]
      :or {account-page unimplemented-page
           confirm-failed-page unimplemented-page}}
     {:keys [confirmation-email was-authenticated was-invalidated]
      :or {confirmation-email (basic-email base-path)
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
            (GET "/logout" [:as {session :session}]
                 (was-invalidated session)))))
