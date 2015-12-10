(ns com.beardandcode.users.integration.login-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users :as users]
            [com.beardandcode.users.integration :as i]))

(def system (atom nil))

(use-fixtures :each (i/wrap-test system))
(use-fixtures :once (i/store-system! system))

(defn- login [email-address password]
  (wd/to (i/url @system "/account"))
  (wd/quick-fill-submit
   {"input[name=\"email-address\"]" email-address}
   {"input[name=password]" password}
   {"input[name=password]" wd/submit}))

(defmacro assert-path [path]
  `(is (= (i/current-path @system) ~path)))

(defmacro assert-errors [selector errors]
  `(is (= (map wd/text (wd/elements ~selector))
          (map users/text ~errors))))

(deftest login-appears
  (wd/to (i/url @system "/account/"))
  (is (= (wd/text "#email-address")
         "Email address")))

(deftest login-fails
  (login "nota@user.com" "wontwork")
  (assert-path "/account/login")
  (assert-errors "form[action=\"/account/login\"] > .error" [:no-user]))

(deftest login-missing-email-address
  (login "" "password")
  (assert-path "/account/login")
  (assert-errors "#email-address > .error" [:required])
  (assert-errors "#password > .error" []))

(deftest login-missing-password
  (login "an@email.address" "")
  (assert-path "/account/login")
  (assert-errors "#email-address > .error" [])
  (assert-errors "#password > .error" [:required]))

(deftest login-email-address-remembered
  (login "an@email.address" "")
  (assert-path "/account/login")
  (is (= (map wd/value (wd/elements "input[name=\"email-address\"]"))
         ["an@email.address"])))

(deftest login-invalid-email-address
  (login "not-an-email" "asd")
  (assert-path "/account/login")
  (assert-errors "#email-address > .error" [:invalid-email]))

(deftest login-successful
  (login "a@user.com" "password")
  (assert-path "/")
  (is (= (wd/text ".status") "Authenticated")))
