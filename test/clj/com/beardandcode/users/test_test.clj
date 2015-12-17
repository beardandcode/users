(ns com.beardandcode.users.test-test
  (:require [clojure.test :refer :all]
            [com.beardandcode.users.store :as store]
            [com.beardandcode.users.test :refer :all]))

(deftest users-are-cleaned-up
  (let [user-store (store/new-mem-store)
        email-address "some@user.com"
        password "asdf"]
    (is (not (store/authenticate user-store email-address password)))
    (with-users user-store [_ {:email-address email-address :password password}]
      (is (store/authenticate user-store email-address password)))
    (is (not (store/authenticate user-store email-address password)))))

(deftest users-not-confirmed-by-default
  (let [user-store (store/new-mem-store)]
    (with-users user-store [a-user {:email-address "a@b.com" :password "a"}]
      (is (not (store/confirmed? user-store a-user))))))

(deftest users-can-be-confirmed
  (let [user-store (store/new-mem-store)]
    (with-users user-store [a-user {:email-address "b@c.com" :password "b" :confirmed? true}]
      (is (store/confirmed? user-store a-user)))))
