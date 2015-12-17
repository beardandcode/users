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
   :no-account "Please check your email address as there appears to be no account with it."
   :failed-to-reset "Failed to reset your password, sorry."})

(defn login [user-store success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/login)]
      (if-let [errors (forms/errors request schemata/login)]
        (fail-fn request {:errors errors :values values})
        (if-let [user (store/authenticate user-store (values "email-address")
                                          (values "password"))]
          (if (store/confirmed? user-store user)
            (success-fn request user)
            (fail-fn request {:errors {"/" [:not-confirmed]} :values values}))
          (fail-fn request {:errors {"/" [:no-user]} :values values}))))))

(defn register [user-store email-service email-fn success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/register)]
      (if-let [errors (forms/errors request schemata/register)]
        (fail-fn request {:errors errors :values values})
        (if (= (values "password") (values "repeat-password"))
          (if-let [user (store/register! user-store (values "email-address")
                                         (values "password") (values "name"))]
            (let [confirmation-token (store/confirmation-token! user-store user)
                  generated-email (email-fn user confirmation-token)]
              (send-email email-service (:email-address user)
                          (:subject generated-email) (:message generated-email))
              (success-fn request user))
            (fail-fn request {:errors {"/" [:failed-to-register]} :values values}))
          (fail-fn request {:errors {"/" [:passwords-dont-match]} :values values}))))))

(defn confirm [user-store success-fn fail-fn]
  (fn [request]
    (if-let [user (store/confirm! user-store (-> request :params :token))]
      (success-fn request user)
      (fail-fn request))))

(defn request-reset [user-store email-service email-fn success-fn fail-fn]
  (fn [request]
    (let [values (forms/values request schemata/request-reset)]
      (if-let [errors (forms/errors request schemata/request-reset)]
        (fail-fn request {:errors errors :values values})
        (if-let [user (store/find-user user-store (values "email-address"))]
          (let [reset-token (store/reset-password-token! user-store user)
                generated-email (email-fn user reset-token)]
            (send-email email-service (:email-address user) (:subject generated-email)
                        (:message generated-email))
            (success-fn request user))
          (fail-fn request {:errors {"/" [:no-account]} :values values}))))))

(def valid-reset-token? store/valid-reset-token?)

(defn reset-password [user-store success-fn bad-token-fn fail-fn]
  (fn [request]
    (if-let [token (-> request :params :token)]
      (if (valid-reset-token? user-store token)
        (let [values (forms/values request schemata/reset-password)]
          (if-let [errors (forms/errors request schemata/reset-password)]
            (fail-fn request {:errors errors :values values})
            (if (= (values "password") (values "repeat-password"))
              (if-let [user (store/reset-password! user-store token (values "password"))]
                (success-fn request user)
                (fail-fn request {:errors {"/" [:failed-to-reset]}}))
              (fail-fn request {:errors {"/" [:passwords-dont-match]}}))))
          (bad-token-fn request))
        (bad-token-fn request))))
