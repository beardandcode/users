(ns com.beardandcode.users.routes
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [com.beardandcode.users :as users]))

(defn- unimplemented-page [& _]
  "<h1>Unimplemented page</h1>")

(defn mount
  ([b u p e] (mount b u p e {}))
  ([base-path user-store email-service
     {:keys [account-page]
      :or {account-page unimplemented-page}}
     {:keys [was-authenticated was-invalidated]
      :or {was-authenticated (fn [session] (fn [user]
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
                  (users/register user-store email-service
                                  (was-authenticated session)
                                  #(account-page {} %)))
            (GET "/logout" [:as {session :session}]
                 (was-invalidated session)))))
