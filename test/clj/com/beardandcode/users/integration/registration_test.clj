(ns com.beardandcode.users.integration.registration-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :as i]
            [com.beardandcode.users.test :as test]))

(def system (atom nil))

(use-fixtures :each (i/wrap-test system))
(use-fixtures :once (i/store-system! system))

(defn- register [email-address name password repeat-password]
  (wd/to (i/url @system "/account"))
  (wd/quick-fill-submit
   {"input[name=\"email-address\"]" email-address}
   {"input[name=name]" name}
   {"input[name=password]" password}
   {"input[name=\"repeat-password\"]" repeat-password}
   {"input[name=\"repeat-password\"]" wd/submit}))

(deftest register-missing-email
  (register "" "" "a" "a")
  (i/assert-path system "/account/register")
  (i/assert-errors "#email-address > .error" [:required]))

(deftest register-missing-both-passwords
  (register "foo@bar.com" "" "" "")
  (i/assert-path system "/account/register")
  (i/assert-errors "#password > .error" [:required])
  (i/assert-errors "#repeat-password > .error" [:required]))

(deftest register-password-mismatch
  (register "foo@bar.com" "" "a" "b")
  (i/assert-path system "/account/register")
  (i/assert-errors "form[action=\"/account/register\"] > .error" [:passwords-dont-match]))

(deftest register-email-and-name-are-remembered
  (register "foo@bar.com" "Foo Bar" "a" "")
  (i/assert-path system "/account/register")
  (is (= (wd/value "#register input[name=\"email-address\"]") "foo@bar.com"))
  (is (= (wd/value "#register input[name=name]") "Foo Bar"))
  (is (= (wd/value "#register input[name=password]") "")))

(deftest register-fails-if-already-exists
  (test/with-users (:user-store @system) [_ {:username "a@b.com" :password "a"}]
    (register "a@b.com" "" "a" "a")
    (i/assert-path system "/account/register")
    (i/assert-errors "form[action=\"/account/register\"] > .error" [:failed-to-register])))

(deftest register-succeeds
  (register "a@user.com" "A User" "asdf" "asdf")
  (wd/take-screenshot :file "/tmp/error.png")
  (i/assert-path system "/")
  (is (= (wd/text ".status") "Authenticated")))


