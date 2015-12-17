(ns com.beardandcode.users.integration.reset-password-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :refer :all]
            [com.beardandcode.users.test :refer :all]))

(def system (atom nil))

(use-fixtures :each (wrap-test system))
(use-fixtures :once (store-system! system))

(defn- request-reset [email-address]
  (wd/to (url @system "/account/reset-password"))
  (wd/quick-fill-submit
   {"input[name=\"email-address\"]" email-address}
   {"input[name=\"email-address\"]" wd/submit}))

(defn- reset-password [password repeat-password]
  (wd/to (url @system (get-path-from-email system)))
  (wd/quick-fill-submit
   {"input[name=password]" password}
   {"input[name=\"repeat-password\"]" repeat-password}
   {"input[name=\"repeat-password\"]" wd/submit}))

(deftest email-address-is-required
  (request-reset "")
  (assert-path system "/account/reset-password")
  (assert-errors "#email-address > .error" [:required]))

(deftest email-address-has-to-exist
  (request-reset "no@user.com")
  (assert-path system "/account/reset-password")
  (assert-errors "form > .error" [:no-account]))

(deftest has-to-be-an-email-address
  (request-reset "not-an-email")
  (assert-path system "/account/reset-password")
  (assert-errors "#email-address > .error" [:invalid-email]))

(deftest user-gets-an-email
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "a" :confirmed? true}]
    (request-reset "a@user.com")
    (let [emails (list-emails system)]
      (is (= (count emails) 1))
      (is (= (-> emails first :to) "a@user.com"))
      (is (-> emails first :subject string?))
      (is (-> emails first :message :text string?)))))

(deftest passwords-have-to-match
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "a" :confirmed? true}]
    (request-reset "a@user.com")
    (reset-password "b" "c")
    (assert-path system #"^/account/reset-password/[0-9]+$")
    (assert-errors "form > .error" [:passwords-dont-match])))

(deftest passwords-are-required
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "a" :confirmed? true}]
    (request-reset "a@user.com")
    (reset-password "" "")
    (assert-path system #"^/account/reset-password/[0-9]+$")
    (assert-errors "#password > .error" [:required])
    (assert-errors "#repeat-password > .error" [:required])))

(deftest user-can-reset-their-email
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "a" :confirmed? true}]
    (request-reset "a@user.com")
    (reset-password "b" "b")
    (assert-path system "/")
    (assert-authenticated)
    (logout system)
    (login system "a@user.com" "b")
    (assert-path system "/")
    (assert-authenticated)))

(deftest if-not-confirmed-reset-confirms-them
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "a"}]
    (request-reset "a@user.com")
    (reset-password "b" "b")
    (assert-path system "/")
    (assert-authenticated)
    (logout system)
    (login system "a@user.com" "b")
    (assert-path system "/")
    (assert-authenticated)))

(deftest bad-token-fails
  (wd/to (url @system "/account/reset-password/bad-token"))
  (assert-path system "/account/reset-password/bad-token")
  (is (= 0 (-> "form[action=\"/account/reset-password/bad-token\"]" wd/elements count))))
