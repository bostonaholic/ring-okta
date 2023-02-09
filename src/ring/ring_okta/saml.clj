(ns ring.ring-okta.saml
  (:require [clojure.data.codec.base64 :as b64]
            [clojure.string :as string])
  (:import (com.okta.saml SAMLValidator)))

(defn- get-saml-response [saml-response okta-config validator]
  (let [config (.getConfiguration validator okta-config)
        decoded-saml-response (String. (b64/decode (.getBytes saml-response "UTF-8")))]
    (.getSAMLResponse validator decoded-saml-response config)))

(defn- get-valid-user-id [saml-response okta-config validator]
  (let [valid-saml-response (get-saml-response saml-response okta-config validator)]
    (.getUserID valid-saml-response)))

(defn respond-to-okta-post [okta-config params]
  {:redirect-url (:RelayState params)
   :authenticated-user-email (string/lower-case
                              (get-valid-user-id (:SAMLResponse params) okta-config (SAMLValidator.)))})
