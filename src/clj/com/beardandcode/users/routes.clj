(ns com.beardandcode.users.routes
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [com.beardandcode.users :as users]))

(defn- unimplemented-page [& args]
  (str args))

(defn mount
  ([b u e] (mount b u e {} {}))
  ([b u e p] (mount b u e p {}))
  ([base-path user-store email-service
    {:keys [account-page confirm-failed-page
            reset-password-page reset-password-failed-page]
     :or {account-page unimplemented-page
          confirm-failed-page #(redirect "/")
          reset-password-page unimplemented-page
          reset-password-failed-page #(redirect "/")}}
    {:keys [was-authenticated was-invalidated]
      :or {was-authenticated (fn [session] (fn [user redirect-path]
                                            (-> (redirect (or redirect-path "/"))
                                                (assoc :session (assoc session :identity user)))))
           was-invalidated (fn [session] (fn [_]
                                          (-> (redirect "/")
                                              (assoc :session (dissoc session :identity)))))}}]
    (context base-path []
             (GET "/" [] (account-page {} {}))
             (POST "/login" [:as {session :session}]
                   (users/login user-store (was-authenticated session)
                          #(account-page % {})))
             (POST "/register" [:as {session :session}]
                   (users/register user-store email-service
                             (was-authenticated session)
                             #(account-page {} %)))
             (GET "/confirm/:token" [:as {session :session}]
                  (users/confirm user-store (was-authenticated session)
                           confirm-failed-page))
             (GET "/reset-password" [email-address]
                  (reset-password-page {:values {"email-address" email-address}}))
             (POST "/reset-password" []
                   (users/send-reset-password-token user-store email-service
                                              reset-password-failed-page
                                              reset-password-page))
             (GET "/reset-password/:token" [:as {session :session}]
                  (users/reset-password user-store (was-authenticated session)
                                  reset-password-failed-page))
             (GET "/logout" [:as {session :session}]
                  (was-invalidated session)))))
