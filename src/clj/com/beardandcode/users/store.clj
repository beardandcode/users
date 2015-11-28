(ns com.beardandcode.users.store)

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
  
  (reset-password-token! [_ email-address password]
    "Creates a reset password token for the user with the given email address")
  (reset-password! [_ token]
    "Reset a password based on a token from requesting to reset"))

(defrecord MemStore [users confirmation-tokens reset-tokens]
  IUserStore
  (authenticate [_ email-address password]
    (@users (str email-address ":" password)))
  (register! [store email-address password name]
    (let [new-user {:email-address email-address
                    :password password
                    :name name
                    :confirmed? false}]
      (swap! users assoc (str email-address ":" password) new-user)
      new-user))

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

  (reset-password-token! [_ email-address new-password]
    (if-let [user-id (->> (keys @users)
                          (filter #(.startsWith %1 (str email-address ":")))
                          first)]
      (let [token (str (System/currentTimeMillis))]
        (swap! reset-tokens assoc token {:id user-id
                                         :new-password new-password})
        token)))
  (reset-password! [_ token]
    (if-let [request (@reset-tokens token)]
      (let [user (@users (:id request))
            reset-user (assoc user :password (:new-password request))]
        (swap! users assoc (str (:email-address reset-user) ":" (:password reset-user)) reset-user)
        (swap! users dissoc (:id request))
        (swap! reset-tokens dissoc token)
        reset-user))))

(defn new-mem-store [users]
  (MemStore. (atom (reduce (fn [users-map [email-address password name]]
                             (assoc users-map (str email-address ":" password)
                                    {:email-address email-address :password password :name name :confirmed? true}))
                           {} users))
             (atom {}) (atom {})))
