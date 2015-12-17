(ns com.beardandcode.users.integration
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.stuartsierra.component :as component]
            [com.beardandcode.components.email.mock :as email-mock]
            [com.beardandcode.components.session.mock :as session-mock]
            [com.beardandcode.components.web-server :as web-server]
            [com.beardandcode.users.example :as webapp]
            [com.beardandcode.users.store.mock :as store-mock]))

(def ^:private browser-count (atom 0))

(defn browser-retain []
  (when (= 1 (swap! browser-count inc))
    (wd/set-driver! {:browser :phantomjs})))

(defn browser-release [& {:keys [force] :or {force false}}]
  (when (zero? (swap! browser-count (if force (constantly 0) dec)))
            (wd/quit)))

(defn wrap-test [system]
  (fn [test-fn]
    (browser-retain)
    (-> @system :session-store session-mock/clear-sessions)
    (-> @system :email-service email-mock/clear-emails)
    (-> @system :user-store store-mock/clear-users)
    (test-fn)
    (browser-release)))

(defn store-system! [system]
  (fn [ns-fn]
    (let [system-map (webapp/new-test-system 0)]
      (reset! system (component/start system-map))
      (ns-fn)
      (component/stop @system))))

(defn url
  ([system] (url system "/"))
  ([system path]
   (str "http://127.0.0.1:" (-> system :web web-server/port) path)))

(defn current-path [system]
  (let [base-url (url system "")
        base-re (re-pattern base-url)
        current-url (wd/current-url)]
    (clojure.string/replace-first current-url base-re "")))

(defn list-emails [system]
  (-> @system :email-service email-mock/list-emails))

(defn get-path-from-email
  ([system] (get-path-from-email system first))
  ([system which-email-fn]
   (let [emails (list-emails system)
         email-body (-> emails which-email-fn :message :text)]
     (nth (re-find #"(?m)^https?://[^/]+(/.*)$" email-body) 1))))

(defmacro assert-path [system path]
  `(let [~'current-path (current-path @~system)]
     (if (instance? java.util.regex.Pattern ~path)
       (is (re-matches ~path ~'current-path))
       (is (= ~path ~'current-path)))))

(defmacro assert-errors [selector errors]
  `(is (= (map wd/text (wd/elements ~selector))
          (map com.beardandcode.users/text ~errors))))

(defmacro assert-authenticated []
  `(is (= (wd/text ".status") "Logout")))
(defmacro assert-unauthenticated []
  `(is (= (wd/text ".status") "Login / register")))

(defn login [system email-address password]
  (wd/to (url @system "/account"))
  (wd/quick-fill-submit
   {"#login input[name=\"email-address\"]" email-address}
   {"#login input[name=password]" password}
   {"#login input[name=password]" wd/submit}))

(defn register [system email-address name password repeat-password]
  (wd/to (url @system "/account"))
  (wd/quick-fill-submit
   {"input[name=\"email-address\"]" email-address}
   {"input[name=name]" name}
   {"input[name=password]" password}
   {"input[name=\"repeat-password\"]" repeat-password}
   {"input[name=\"repeat-password\"]" wd/submit}))

(defn logout [system]
  (wd/to (url @system "/account/logout")))
