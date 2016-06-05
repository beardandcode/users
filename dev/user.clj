(ns user
  (:require [clojure.java.shell :as shell]
            [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as ragtime]
            [reloaded.repl :refer [system init start stop go reset clear]]
            [vinyasa.reimport :refer [reimport]]
            [vinyasa.pull :refer [pull]]
            [com.beardandcode.components.database :refer [normalise-url]]
            [com.beardandcode.components.web-server :refer [port]]
            [com.beardandcode.users :refer :all]
            [com.beardandcode.users.example :refer [new-test-system]]))

(def default-connection-uri "jdbc:postgresql://127.0.0.1:5432/users?user=postgres&password=password")
(def connection-uri (normalise-url (or (System/getenv "DATABASE_URL") default-connection-uri)))
(def ragtime-config
  {:datastore (jdbc/sql-database {:connection-uri connection-uri})
   :migrations (jdbc/load-resources "migrations/com/beardandcode/users")})

(reloaded.repl/set-init!
 #(new-test-system (Integer. (or (System/getenv "PORT") 8080)) connection-uri))

(defn migrate [] (ragtime/migrate ragtime-config))
(defn rollback [] (ragtime/rollback ragtime-config))

(defn url [] (str "http://localhost:" (-> system :web port) "/"))
(defn open! [] (shell/sh "open" (url)))

(defn refresh-and [f]
  (refresh :after (symbol "user" f)))

(defn test-all [] (run-all-tests #"^com.beardandcode.users.*-test$"))
(defn test-unit [] (run-all-tests #"^com.beardandcode.users.(?!integration).*-test$"))
(defn test-integration [] (run-all-tests #"^com.beardandcode.users.integration.*-test$"))
