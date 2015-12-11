(ns com.beardandcode.users.integration.confirmation-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :refer :all]
            [com.beardandcode.users.test :refer :all]))

(def system (atom nil))

(use-fixtures :each (wrap-test system))
(use-fixtures :once (store-system! system))

(deftest confirmation-email-gets-sent
  (register system "some@email.com" "some body" "a" "a")
  (assert-path system "/")
  (let [emails (list-emails system)]
    (is (= (count emails) 1))
    (is (= (-> emails first :to) "some@email.com"))
    (is (string? (-> emails first :subject)))
    (is (string? (-> emails first :message :text)))))

(deftest cannot-login-until-confirmed
  (register system "a.person@email.com" "" "a" "a")
  (assert-path system "/")
  (logout system)
  (login system "a.person@email.com" "a")
  (assert-path system "/account/login")
  (assert-errors "form[action=\"/account/login\"] > .error" [:not-confirmed]))

(deftest email-url-logs-in-and-confirms
  (register system "a@user.com" "" "a" "a")
  (assert-path system "/")
  (logout system)
  (let [emails (list-emails system)
        confirm-text-body (-> emails first :message :text)
        confirm-path (nth (re-find #"(?m)^https?://[^/]+(/.*)$" confirm-text-body) 1)]
    (wd/to (url @system confirm-path))
    (assert-path system "/")
    (is (= (wd/text ".status") "Authenticated"))
    (logout system)
    (is (= (wd/text ".status") "Unauthenticated"))
    (login system "a@user.com" "a")
    (assert-path system "/")
    (is (= (wd/text ".status") "Authenticated"))))

(deftest cant-use-token-twice
  (register system "b@user.com" "" "a" "a")
  (assert-path system "/")
  (logout system)
  (let [emails (list-emails system)
        confirm-text-body (-> emails first :message :text)
        confirm-path (nth (re-find #"(?m)^https?://[^/]+(/.*)$" confirm-text-body) 1)]
    (wd/to (url @system confirm-path))
    (assert-path system "/")
    (is (= (wd/text ".status") "Authenticated"))
    (logout system)
    (wd/to (url @system confirm-path))
    (assert-path system #"/account/confirm/[\d]+")))

(deftest bad-token
  (wd/to (url @system "/account/confirm/123"))
  (assert-path system "/account/confirm/123"))
