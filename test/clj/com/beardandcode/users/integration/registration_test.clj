(ns com.beardandcode.users.integration.registration-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :refer :all]
            [com.beardandcode.users.test :refer :all]))

(def system (atom nil))

(use-fixtures :each (wrap-test system))
(use-fixtures :once (store-system! system))

(deftest register-missing-email
  (register system "" "" "a" "a")
  (assert-path system "/account/register")
  (assert-errors "#email-address > .error" [:required]))

(deftest register-missing-both-passwords
  (register system "foo@bar.com" "" "" "")
  (assert-path system "/account/register")
  (assert-errors "#password > .error" [:required])
  (assert-errors "#repeat-password > .error" [:required]))

(deftest register-password-mismatch
  (register system "foo@bar.com" "" "a" "b")
  (assert-path system "/account/register")
  (assert-errors "form[action=\"/account/register\"] > .error" [:passwords-dont-match]))

(deftest register-email-and-name-are-remembered
  (register system "foo@bar.com" "Foo Bar" "a" "")
  (assert-path system "/account/register")
  (is (= (wd/value "#register input[name=\"email-address\"]") "foo@bar.com"))
  (is (= (wd/value "#register input[name=name]") "Foo Bar"))
  (is (= (wd/value "#register input[name=password]") "")))

(deftest register-fails-if-already-exists
  (with-users (:user-store @system) [_ {:email-address "a@b.com" :password "a"}]
    (register system "a@b.com" "" "a" "a")
    (assert-path system "/account/register")
    (assert-errors "form[action=\"/account/register\"] > .error" [:failed-to-register])))

(deftest register-succeeds
  (register system "a@user.com" "A User" "asdf" "asdf")
  (wd/take-screenshot :file "/tmp/error.png")
  (assert-path system "/")
  (assert-authenticated))


