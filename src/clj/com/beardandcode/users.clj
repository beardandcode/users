(ns com.beardandcode.users
  (:require [com.beardandcode.forms :as forms]
            [com.beardandcode.users.schemata :as schemata]
            [com.beardandcode.users.store :as store]))

(def text
  {:required "Please fill out this field."
   :invalid-email "Please enter a valid email address."
   :not-confirmed "You haven't confirmed your account, please check your email."
   :no-user "We can't find a user with that email address and password."
   :failed-to-register "Failed to register your account, sorry."
   :passwords-dont-match "The two passwords you entered do not match."})

(defn login [store-instance success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/login)]
      (if-let [errors (forms/errors request schemata/login)]
        (fail-fn {:errors errors :values values})
        (if-let [user (store/authenticate store-instance (values "email-address")
                                          (values "password"))]
          (if (store/confirmed? store-instance user)
            (success-fn user)
            (fail-fn {:errors {"/" [:not-confirmed]} :values values}))
          (fail-fn {:errors {"/" [:no-user]} :values values}))))))

(defn register [store-instance success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/register)]
      (if-let [errors (forms/errors request schemata/register)]
        (fail-fn {:errors errors :values values})
        (if (= (values "password") (values "repeat-password"))
          (if-let [user (store/register! store-instance (values "email-address")
                                         (values "password") (values "name"))]
            (success-fn user)
            (fail-fn {:errors {"/" [:failed-to-register]} :values values}))
          (fail-fn {:errors {"/" [:passswords-dont-match]} :values values}))))))
