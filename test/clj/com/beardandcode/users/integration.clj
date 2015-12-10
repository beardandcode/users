(ns com.beardandcode.users.integration
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.stuartsierra.component :as component]
            [com.beardandcode.components.session.mock :as session-mock]
            [com.beardandcode.components.web-server :as web-server]
            [com.beardandcode.users.example.webapp :as webapp]))

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

(defmacro assert-path [system path]
  `(is (= (current-path @~system) ~path)))

(defmacro assert-errors [selector errors]
  `(is (= (map wd/text (wd/elements ~selector))
          (map com.beardandcode.users/text ~errors))))

(defn login [system email-address password]
  (wd/to (url @system "/account"))
  (wd/quick-fill-submit
   {"#login input[name=\"email-address\"]" email-address}
   {"#login input[name=password]" password}
   {"#login input[name=password]" wd/submit}))
