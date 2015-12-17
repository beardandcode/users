(ns com.beardandcode.users.test
  (:require [com.beardandcode.users.store :as store]))

(defn- all-parity [parity-fn coll]
  (->> coll
       (map-indexed vector)
       (filter #(-> % first parity-fn))
       (mapv second)))

(defn- all-evens [coll] (all-parity even? coll))
(defn- all-odds [coll] (all-parity odd? coll))

(defn register-from-map! [user-store user-map]
  (store/register! user-store
                   (:email-address user-map)
                   (:password user-map)
                   (get user-map :name "name")))

(defmacro with-users [store bindings & body]
  (cond
    (-> bindings count odd?) (throw (Exception. "Need to have even bindings."))
    
    (not (every? #(and (map? %)
                       (-> #{:email-address :password}
                           (clojure.set/difference (-> % keys set))
                           count zero?))
                 (all-odds bindings)))
    (throw (Exception. "User values have to be maps with :email-address, :password and optionally :name keys."))
    
    :else
    `(let ~(into [] (reduce concat [] (map (fn [[sym args]]
                                             (let [user-sym (gensym) token-sym (gensym)]
                                               [sym (if (:confirmed? args)
                                                      `(let [~user-sym (register-from-map! ~store ~args)
                                                             ~token-sym (store/confirmation-token! ~store ~user-sym)]
                                                         (store/confirm! ~store ~token-sym))
                                                      `(register-from-map! ~store ~args))]))
                                           (partition 2 bindings))))
       ~@body
       ~(let [user-sym (gensym)]
          `(doseq [~user-sym ~(all-evens bindings)]
             (store/delete! ~store ~user-sym))))))
