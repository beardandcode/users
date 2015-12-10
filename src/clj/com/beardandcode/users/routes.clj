(ns com.beardandcode.users.routes
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [com.beardandcode.users :as users]))

(defn- unimplemented-page [& _]
  "<h1>Unimplemented page</h1>")

(defn mount
  ([b u p] (mount b u p {}))
  ([base-path user-store
     {:keys [account-page]
      :or {account-page unimplemented-page}}
     {:keys [was-authenticated]
      :or {was-authenticated (fn [session] (fn [user]
                                            (-> (redirect "/")
                                                (assoc :session (assoc session :identity user)))))}}]
   (context base-path []
            (GET "/" [] (account-page {}))
            (POST "/login" [:as {session :session}]
                  (users/login user-store (was-authenticated session)
                               #(account-page %))))))
