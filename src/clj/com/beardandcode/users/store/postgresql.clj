(ns com.beardandcode.users.store.postgresql
  (:import [org.postgresql.util PSQLException])
  (:require [com.beardandcode.components.database :refer [defqueries with-transaction]]
            [com.beardandcode.users.store :refer [IUserStore]]))

(defqueries "sql/com/beardandcode/users.sql")

(def allowed-user-keys #{:id :email_address :name :confirmed :created_at :updated_at})
(defn- restrict-user-keys [user]
  (when (-> user nil? not)
    (reduce #(assoc %1 %2 (user %2)) {} allowed-user-keys)))
(defn- clojurify-keys [user]
  (when (-> user nil? not)
    (reduce (fn [out [k v]] (assoc out (-> k name (clojure.string/replace #"_" "-") keyword) v))
            {} user)))

(defrecord PostgreSQLStore [db]
  IUserStore
  (authenticate [_ email-address password]
    (-> (authenticate-user db {:email_address email-address :password password})
        first restrict-user-keys clojurify-keys))
  (register! [_ email-address password name]
    (try
      (-> (register-user<! db {:email_address email-address :password password :name name})
          restrict-user-keys clojurify-keys)
      (catch PSQLException e
        (if (.contains (.getMessage e) "violates unique")
          nil (.printStackTrace e)))))

  (confirmation-token! [_ user]
    (:token (create-token<! db {:user_id (:id user) :token_type "confirmation"})))
  (confirmed? [_ user]
    (= (:confirmed user) true))
  (confirm! [_ token]
    (with-transaction [transaction-conn db]
      (if-let [user (confirm-user<! transaction-conn {:token token})]
        (do (delete-token! transaction-conn {:token token})
            (-> user restrict-user-keys clojurify-keys)))))

  (find-user [_ email-address]
    (-> (get-user-by-email-address db {:email_address email-address})
        first restrict-user-keys clojurify-keys))
  (reset-password-token! [_ user]
    (:token (create-token<! db {:user_id (:id user) :token_type "reset"})))
  (valid-reset-token? [_ token]
    (= (count (find-token db {:token_type "reset" :token token})) 1))
  (reset-password! [_ token password]
    (with-transaction [transaction-conn db]
      (if-let [user (reset-user-password<! transaction-conn {:token token :password password})]
        (do (delete-token! transaction-conn {:token token})
            (-> user restrict-user-keys clojurify-keys)))))

  (delete! [_ user]
    (delete-user-by-id! db {:id (:id user)})))

(defn new-store []
  (map->PostgreSQLStore {}))
