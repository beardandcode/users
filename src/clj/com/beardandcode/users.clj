(ns com.beardandcode.users
  (:require [com.beardandcode.components.email :refer [send-email]]
            [com.beardandcode.forms :as forms]
            [com.beardandcode.users.schemata :as schemata]
            [com.beardandcode.users.store :as store]))

(def text
  {:required "Please fill out this field."
   :invalid-email "Please enter a valid email address."
   :not-confirmed "You haven't confirmed your account, please check your email."
   :no-user "We can't find a user with that email address and password."
   :failed-to-register "Failed to register your account, sorry."
   :passwords-dont-match "The two passwords you entered do not match."
   :no-reset-password "We couldn't change your password."})

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

(defn register [store-instance email-service success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/register)]
      (if-let [errors (forms/errors request schemata/register)]
        (fail-fn {:errors errors :values values})
        (if (= (values "password") (values "repeat-password"))
          (if-let [user (store/register! store-instance (values "email-address")
                                         (values "password") (values "name"))]
            (let [confirmation-token (store/confirmation-token! store-instance user)]
              (send-email email-service (:email-address user) "Confirm token" confirmation-token)
              (success-fn user))
            (fail-fn {:errors {"/" [:failed-to-register]} :values values}))
          (fail-fn {:errors {"/" [:passswords-dont-match]} :values values}))))))

(defn confirm [store-instance success-fn fail-fn]
  (fn [request]
    (if-let [user (store/confirm! store-instance (-> request :params :token))]
      (success-fn user)
      (fail-fn))))

(defn confirm-forgotten-password [store-instance email-service success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/forgotten-password)]
      (if-let [errors (forms/errors request schemata/forgotten-password)]
        (fail-fn {:errors errors :values values})
        (if (= (values "password") (values "repeat-password"))
          (if-let [token (store/reset-password-token! store-instance (values "email-address") (values "password"))]
            (do (send-email email-service (values "email-address") "Forgotten password" token)
                (success-fn))
            (fail-fn {:errors {"/" [:no-reset-password]}}))
          (fail-fn {:errors {"/" [:passwords-dont-match]} :values values}))))))

(defn forgotten-password [store-instance success-fn fail-fn]
  (fn [request]
    (if-let [user (store/reset-password! store-instance (-> request :params :token))]
      (success-fn user)
      (fail-fn))))
