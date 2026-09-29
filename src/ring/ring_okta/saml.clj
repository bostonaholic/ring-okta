(ns ring.ring-okta.saml
  (:require [clojure.string :as string])
  (:import (com.onelogin.saml2.authn SamlResponse)
           (com.onelogin.saml2.settings Saml2Settings SettingsBuilder)
           (java.io StringReader)
           (javax.xml.namespace NamespaceContext)
           (javax.xml.parsers DocumentBuilderFactory)
           (javax.xml.xpath XPath XPathConstants XPathFactory)
           (org.w3c.dom Document Node)
           (org.xml.sax InputSource)))

;; Clojure 1.9 reflection on JDK 17+ resolves interop calls to non-exported
;; JDK implementation classes and fails, so every call must be hinted.
(set! *warn-on-reflection* true)

(def ^:private application-path "/configuration/applications/application")
(def ^:private idp-entity-id-path "md:EntityDescriptor/@entityID")
(def ^:private idp-certificate-path
  "md:EntityDescriptor/md:IDPSSODescriptor/md:KeyDescriptor[not(@use) or @use='signing']//ds:X509Certificate")
(def ^:private idp-sso-url-path
  "md:EntityDescriptor/md:IDPSSODescriptor/md:SingleSignOnService[@Binding='urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST']/@Location")
(def ^:private sp-entity-id-path "sp/@entityID")
(def ^:private sp-acs-url-path "sp/@assertionConsumerServiceURL")

(def ^:private namespace-uris
  {"md" "urn:oasis:names:tc:SAML:2.0:metadata"
   "ds" "http://www.w3.org/2000/09/xmldsig#"})

(def ^:private namespace-context
  (reify NamespaceContext
    (getNamespaceURI [_ prefix] (get namespace-uris prefix))
    (getPrefix [_ _] nil)
    (getPrefixes [_ _] nil)))

(defn- parse-xml ^Document [^String xml]
  (let [factory (doto (DocumentBuilderFactory/newInstance)
                  (.setNamespaceAware true)
                  (.setFeature "http://apache.org/xml/features/disallow-doctype-decl" true))]
    (.parse (.newDocumentBuilder factory) (InputSource. (StringReader. xml)))))

(defn- new-xpath ^XPath []
  (doto (.newXPath (XPathFactory/newInstance))
    (.setNamespaceContext namespace-context)))

(defn- parse-okta-config [okta-config]
  (let [^XPath xpath (new-xpath)
        ^Node application (.evaluate xpath ^String application-path (parse-xml okta-config) XPathConstants/NODE)
        value (fn [^String path]
                (string/trim (.evaluate xpath path application)))]
    {:idp-entity-id (value idp-entity-id-path)
     :idp-certificate (value idp-certificate-path)
     :idp-sso-url (value idp-sso-url-path)
     :sp-entity-id (value sp-entity-id-path)
     :sp-acs-url (value sp-acs-url-path)}))

(defn- saml-settings ^Saml2Settings [{:keys [idp-entity-id idp-certificate idp-sso-url sp-entity-id sp-acs-url]}]
  (-> (SettingsBuilder.)
      (.fromValues {"onelogin.saml2.strict" true
                    "onelogin.saml2.security.want_assertions_signed" false
                    "onelogin.saml2.security.want_messages_signed" false
                    "onelogin.saml2.idp.entityid" idp-entity-id
                    "onelogin.saml2.idp.x509cert" idp-certificate
                    "onelogin.saml2.idp.single_sign_on_service.url" idp-sso-url
                    "onelogin.saml2.sp.entityid" sp-entity-id
                    "onelogin.saml2.sp.assertion_consumer_service.url" sp-acs-url})
      (.build)))

(defn- validated-response ^SamlResponse [settings ^String acs-url ^String saml-response]
  (let [^SamlResponse response (SamlResponse. settings acs-url saml-response)]
    (when-not (.isValid response nil)
      (let [^Exception cause (.getValidationException response)]
        (throw (ex-info (.getMessage cause) {:type ::invalid-saml-response} cause))))
    response))

(defn- authenticated-user-email [okta-config saml-response]
  (let [config (parse-okta-config okta-config)
        ^SamlResponse response (validated-response (saml-settings config) (:sp-acs-url config) saml-response)]
    (string/lower-case (.getNameId response))))

(defn respond-to-okta-post [okta-config params]
  {:redirect-url (:RelayState params)
   :authenticated-user-email (authenticated-user-email okta-config (:SAMLResponse params))})
