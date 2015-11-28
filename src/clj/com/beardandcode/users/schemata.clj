(ns com.beardandcode.users.schemata
  (:require [com.beardandcode.forms :refer [defschema]]))

(defschema login "schema/com/beardandcode/users/login.json")
(defschema register "schema/com/beardandcode/users/register.json")
(defschema forgotten-password "schema/com/beardandcode/users/forgotten-password.json")
