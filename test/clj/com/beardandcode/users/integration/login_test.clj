(ns com.beardandcode.users.integration.login-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :refer :all]
            [com.beardandcode.users.test :refer :all]))

(def system (atom nil))

(use-fixtures :each (wrap-test system))
(use-fixtures :once (store-system! system))

(deftest login-appears
  (wd/to (url @system "/account/"))
  (is (= (wd/text "#email-address")
         "Email address")))

(deftest login-fails
  (login system "nota@user.com" "wontwork")
  (assert-path system "/account/login")
  (assert-errors "form[action=\"/account/login\"] > .error" [:no-user]))

(deftest login-missing-email-address
  (login system "" "password")
  (assert-path system "/account/login")
  (assert-errors "#email-address > .error" [:required])
  (assert-errors "#password > .error" []))

(deftest login-missing-password
  (login system "an@email.address" "")
  (assert-path system "/account/login")
  (assert-errors "#email-address > .error" [])
  (assert-errors "#password > .error" [:required]))

(deftest login-email-address-remembered
  (login system "an@email.address" "")
  (assert-path system "/account/login")
  (is (= (map wd/value (wd/elements "#login input[name=\"email-address\"]"))
         ["an@email.address"])))

(deftest login-invalid-email-address
  (login system "not-an-email" "asd")
  (assert-path system "/account/login")
  (assert-errors "#email-address > .error" [:invalid-email]))

(deftest login-successful
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "password" :confirmed? true}]
    (login system "a@user.com" "password")
    (assert-path system "/")
    (assert-authenticated)))
