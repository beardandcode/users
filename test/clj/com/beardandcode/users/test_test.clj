(ns com.beardandcode.users.test-test
  (:require [clojure.test :refer :all]
            [com.beardandcode.users.store :as store]
            [com.beardandcode.users.test :refer :all]))

(deftest users-are-cleaned-up
  (let [user-store (store/new-mem-store)
        username "some@user.com"
        password "asdf"]
    (is (not (store/authenticate user-store username password)))
    (with-users user-store [_ {:username username :password password}]
      (is (store/authenticate user-store username password)))
    (is (not (store/authenticate user-store username password)))))

(deftest users-not-confirmed-by-default
  (let [user-store (store/new-mem-store)]
    (with-users user-store [a-user {:username "a@b.com" :password "a"}]
      (is (not (store/confirmed? user-store a-user))))))

(deftest users-can-be-confirmed
  (let [user-store (store/new-mem-store)]
    (with-users user-store [a-user {:username "b@c.com" :password "b" :confirmed? true}]
      (is (store/confirmed? user-store a-user)))))
