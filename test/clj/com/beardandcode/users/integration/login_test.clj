(ns com.beardandcode.users.integration.login-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.beardandcode.users.integration :as i]))

(def system (atom nil))

(use-fixtures :each i/browser-retain-release)
(use-fixtures :once (i/store-system! system))

(deftest login-appears
  (wd/to (i/url @system "/account/"))
  (is (= (wd/text "#email-address")
         "Email address")))
