(ns com.beardandcode.users.integration.logout-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :refer :all]
            [com.beardandcode.users.test :refer :all]))

(def system (atom nil))

(use-fixtures :each (wrap-test system))
(use-fixtures :once (store-system! system))

(deftest logout-invalidates-session
  (with-users (:user-store @system) [_ {:email-address "a@user.com" :password "a" :confirmed? true}]
    (login system "a@user.com" "a")
    (assert-path system "/")
    (is (= (wd/text ".status") "Authenticated"))
    (logout system)
    (assert-path system "/")
    (is (= (wd/text ".status") "Unauthenticated"))))
