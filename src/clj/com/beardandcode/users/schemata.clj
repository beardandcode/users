(ns com.beardandcode.users.schemata
  (:require [com.beardandcode.forms :refer [defschema]]))

(defschema login "schema/com/beardandcode/users/login.json")
(defschema register "schema/com/beardandcode/users/register.json")
(defschema request-reset "schema/com/beardandcode/users/request-reset.json")
