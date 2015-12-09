(ns com.beardandcode.users.routes
  (:require [ring.util.response :refer [redirect]]
            [compojure.core :refer :all]
            [com.beardandcode.users :as users]))

(defn- unimplemented-page [& _]
  "<h1>Unimplemented page</h1>")

(defn mount [base-path {:keys [account-page]
                        :or {account-page unimplemented-page}}]
  (context base-path []
           (GET "/" [] (account-page))))
