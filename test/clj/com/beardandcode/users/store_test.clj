(ns com.beardandcode.users.store-test
  (:require [clojure.test :refer :all]
            [com.beardandcode.users.store :as store]))

(deftest test-mem-store
  (let [store-instance (store/new-mem-store [["foo@bar.com" "bar" "Foo Bar"]])]
    (is (nil? (store/authenticate store-instance "foo@bar.com" "not-bar")))
    (is (nil? (store/authenticate store-instance "foo@baz.com" "bar")))
    (is (nil? (store/authenticate store-instance "" "")))
    (is (not (nil? (store/authenticate store-instance "foo@bar.com" "bar"))))

    (is (nil? (store/authenticate store-instance "tom@booth.com" "pass")))
    (let [new-user (store/register! store-instance "tom@booth.com" "pass" "Tom")
          user (store/authenticate store-instance "tom@booth.com" "pass")]
      (is (= new-user user))
      (is (not (nil? user)))
      (is (not (store/confirmed? store-instance user)))
      (let [token (store/confirmation-token! store-instance user)]
        (is (not (nil? token)))
        (is (string? token))
        (let [confirmed-user (store/confirm! store-instance token)]
          (is (not (nil? confirmed-user)))
          (is (store/confirmed? store-instance confirmed-user))
          (is (store/confirmed? store-instance (store/authenticate store-instance "tom@booth.com" "pass")))
          (is (not (store/confirm! store-instance token))))))

    (is (nil? (store/register! store-instance "tom@booth.com" "another-pass" "Tomo")))

    (is (not (nil? (store/find-user store-instance "tom@booth.com"))))
    (is (nil? (store/find-user store-instance "notauser@example.com")))

    (is (not (store/valid-reset-token? store-instance "foo")))

    (let [found-user (store/find-user store-instance "tom@booth.com")
          reset-password-token (store/reset-password-token! store-instance found-user)]
      (is (not (nil? reset-password-token)))
      (is (string? reset-password-token))
      (is (store/valid-reset-token? store-instance reset-password-token))
      (let [reset-user (store/reset-password! store-instance reset-password-token "new-pass")
            user (store/authenticate store-instance "tom@booth.com" "new-pass")]
        (is (not (nil? reset-user)))
        (is (= reset-user user))
        (is (not (store/valid-reset-token? store-instance reset-password-token)))
        (is (nil? (store/reset-password! store-instance reset-password-token "other-pass")))
        (is (nil? (store/authenticate store-instance "tom@booth.com" "other-pass")))
        (is (nil? (store/authenticate store-instance "tom@booth.com" "pass")))))

    (let [unconfirmed-user (store/register! store-instance "un@regged.user" "a" "un regged")
          reset-password-token (store/reset-password-token! store-instance unconfirmed-user)
          reset-user (store/reset-password! store-instance reset-password-token "b")]
      (is (store/confirmed? store-instance reset-user)))

    (let [a-user (store/register! store-instance "a@user.com" "asdf" "asd")]
      (is (store/authenticate store-instance "a@user.com" "asdf"))
      (store/delete! store-instance a-user)
      (is (not (store/authenticate store-instance "a@user.com" "asdf"))))

    (is (store/register! store-instance "some@user.com" "a" "b"))
    (is (nil? (store/register! store-instance "some@user.com" "a" "b")))))
