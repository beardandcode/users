(ns com.beardandcode.users.integration
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as wd]
            [com.stuartsierra.component :as component]
            [com.beardandcode.components.web-server :as web-server]
            [com.beardandcode.users.example.webapp :as webapp]))

(def ^:private browser-count (atom 0))

(defn browser-retain []
  (when (= 1 (swap! browser-count inc))
    (wd/set-driver! {:browser :phantomjs})))

(defn browser-release [& {:keys [force] :or {force false}}]
  (when (zero? (swap! browser-count (if force (constantly 0) dec)))
            (wd/quit)))

(defn browser-retain-release [test-fn]
  (browser-retain) (test-fn) (browser-release))

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
