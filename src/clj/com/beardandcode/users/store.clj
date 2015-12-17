(ns com.beardandcode.users.store
  (:require [com.beardandcode.users.store.mock :as mock]))

(defprotocol IUserStore
  (authenticate [_ email-address password]
    "Authenticate a user and return the stored data")
  (register! [_ email-address password name]
    "Registers a user and returns the stored data and a confirmation token")
  
  (confirmation-token! [_ user]
    "Creates a confirmation token for the given user")
  (confirmed? [_ user]
    "Returns whether a user has been confirmed")
  (confirm! [_ token]
    "Confirm a user based on a token created when they registered")

  (find-user [_ email-address]
    "Find a user based on their email address")
  (reset-password-token! [_ user]
    "Creates a reset password token for the user with the given email address")
  (valid-reset-token? [_ token]
    "Return whether a reset token is valid")
  (reset-password! [_ token password]
    "Reset a password based on a token from requesting to reset")

  (delete! [_ user]))

(defrecord MemStore [users confirmation-tokens reset-tokens]
  IUserStore
  (authenticate [_ email-address password]
    (@users (str email-address ":" password)))
  (register! [store email-address password name]
    (let [new-user {:email-address email-address
                    :password password
                    :name name
                    :confirmed? false}]
      (let [user-id (str email-address ":" password)]
        (when (not (find-user store email-address))
          (swap! users assoc user-id new-user)
          new-user))))

  (confirmation-token! [_ user]
    (let [token (str (System/currentTimeMillis))]
      (swap! confirmation-tokens assoc token (str (:email-address user) ":" (:password user)))
      token))
  (confirmed? [_ user] (:confirmed? user))
  (confirm! [_ token]
    (if-let [user-id (@confirmation-tokens token)]
      (let [user (@users user-id)
            confirmed-user (assoc user :confirmed? true)]
        (swap! users assoc user-id confirmed-user)
        (swap! confirmation-tokens dissoc token)
        confirmed-user)))

  (find-user [_ email-address]
    (->> @users
         (filter (fn [[user-id user]]
                   (.startsWith user-id (str email-address ":"))))
         first second))
  (reset-password-token! [_ user]
    (let [token (str (System/currentTimeMillis))]
      (swap! reset-tokens assoc token (str (:email-address user) ":" (:password user)))
      token))
  (valid-reset-token? [_ token]
    (contains? @reset-tokens token))
  (reset-password! [_ token password]
    (if-let [user-id (@reset-tokens token)]
      (let [user (@users user-id)
            reset-user (assoc user :password password :confirmed? true)]
        (swap! users assoc (str (:email-address reset-user) ":" (:password reset-user)) reset-user)
        (swap! users dissoc user-id)
        (swap! reset-tokens dissoc token)
        reset-user)))

  (delete! [_ user]
    (swap! users dissoc (str (:email-address user) ":" (:password user))))

  mock/IMockUserStore
  (clear-users [_] (reset! users {})))

(defn new-mem-store
  ([] (new-mem-store []))
  ([users]
   (MemStore. (atom (reduce (fn [users-map [email-address password name]]
                              (assoc users-map (str email-address ":" password)
                                     {:email-address email-address :password password :name name :confirmed? true}))
                            {} users))
              (atom {}) (atom {}))))
