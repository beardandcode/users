(ns com.beardandcode.users.integration.login-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :as i]
            [com.beardandcode.users.test :as test]))

(def system (atom nil))

(use-fixtures :each (i/wrap-test system))
(use-fixtures :once (i/store-system! system))

(defn- login [email-address password]
  (wd/to (i/url @system "/account"))
  (wd/quick-fill-submit
   {"input[name=\"email-address\"]" email-address}
   {"input[name=password]" password}
   {"input[name=password]" wd/submit}))

(deftest login-appears
  (wd/to (i/url @system "/account/"))
  (is (= (wd/text "#email-address")
         "Email address")))

(deftest login-fails
  (login "nota@user.com" "wontwork")
  (i/assert-path system "/account/login")
  (i/assert-errors "form[action=\"/account/login\"] > .error" [:no-user]))

(deftest login-missing-email-address
  (login "" "password")
  (i/assert-path system "/account/login")
  (i/assert-errors "#email-address > .error" [:required])
  (i/assert-errors "#password > .error" []))

(deftest login-missing-password
  (login "an@email.address" "")
  (i/assert-path system "/account/login")
  (i/assert-errors "#email-address > .error" [])
  (i/assert-errors "#password > .error" [:required]))

(deftest login-email-address-remembered
  (login "an@email.address" "")
  (i/assert-path system "/account/login")
  (is (= (map wd/value (wd/elements "input[name=\"email-address\"]"))
         ["an@email.address"])))

(deftest login-invalid-email-address
  (login "not-an-email" "asd")
  (i/assert-path system "/account/login")
  (i/assert-errors "#email-address > .error" [:invalid-email]))

(deftest login-successful
  (test/with-users (:user-store @system) [_ {:username "a@user.com" :password "password" :confirmed? true}]
    (login "a@user.com" "password")
    (i/assert-path system "/")
    (is (= (wd/text ".status") "Authenticated"))))
