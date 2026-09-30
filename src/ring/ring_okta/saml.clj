(ns ring.ring-okta.saml
  (:require [clojure.string :as string])
  (:import (com.onelogin.saml2.authn SamlResponse)
           (com.onelogin.saml2.settings Saml2Settings SettingsBuilder)
           (java.io StringReader)
           (javax.xml.namespace NamespaceContext)
           (javax.xml.parsers DocumentBuilderFactory)
           (javax.xml.xpath XPath XPathConstants XPathFactory)
           (org.w3c.dom Document Node NodeList)
           (org.xml.sax InputSource)))

;; Clojure 1.9 reflection on JDK 17+ resolves interop calls to non-exported
;; JDK implementation classes and fails, so every call must be hinted.
(set! *warn-on-reflection* true)

(def ^:private application-path "/configuration/applications/application")
(def ^:private idp-certificate-path
  "md:EntityDescriptor/md:IDPSSODescriptor/md:KeyDescriptor[not(@use) or @use='signing']//ds:X509Certificate")

(def ^:private config-paths
  "Each config value and its path relative to application-path, in the
  order a missing-value error names them."
  [[:idp-entity-id "md:EntityDescriptor/@entityID"]
   [:idp-certificate idp-certificate-path]
   [:idp-sso-url "md:EntityDescriptor/md:IDPSSODescriptor/md:SingleSignOnService[@Binding='urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST']/@Location"]
   [:sp-entity-id "sp/@entityID"]
   [:sp-acs-url "sp/@assertionConsumerServiceURL"]])

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

(defn- invalid-okta-config
  ([message] (ex-info message {:type ::invalid-okta-config}))
  ([message cause] (ex-info message {:type ::invalid-okta-config} cause)))

(defn- parse-config-xml ^Document [okta-config]
  (try
    (parse-xml okta-config)
    (catch Exception e
      (throw (invalid-okta-config (str "Okta config is not valid XML: " (.getMessage e)) e)))))

(defn- the-only-node
  "The one node that path selects from context. Throws invalid-okta-config
  with the count when path selects zero or several nodes."
  ^Node [^XPath xpath ^String path context description]
  (let [^NodeList nodes (.evaluate xpath path context XPathConstants/NODESET)
        node-count (.getLength nodes)]
    (when-not (= 1 node-count)
      (throw (invalid-okta-config
              (format "Okta config must have exactly 1 %s at %s, found %d" description path node-count))))
    (.item nodes 0)))

(defn- parse-okta-config [okta-config]
  (let [^XPath xpath (new-xpath)
        ^Node application (the-only-node xpath application-path (parse-config-xml okta-config) "application")
        _ (the-only-node xpath idp-certificate-path application "signing certificate")
        config (into {} (for [[k ^String path] config-paths]
                          [k (string/trim (.evaluate xpath path application))]))
        missing-paths (for [[k path] config-paths
                            :when (string/blank? (get config k))]
                        path)]
    (when (seq missing-paths)
      (throw (invalid-okta-config
              (str "Okta config is missing " (string/join ", " missing-paths) " under " application-path))))
    config))

(defn- build-settings ^Saml2Settings [{:keys [idp-entity-id idp-certificate idp-sso-url sp-entity-id sp-acs-url]}]
  (-> (SettingsBuilder.)
      (.fromValues {"onelogin.saml2.strict" true
                    "onelogin.saml2.security.want_assertions_signed" false
                    "onelogin.saml2.security.want_messages_signed" false
                    "onelogin.saml2.security.reject_deprecated_alg" true
                    "onelogin.saml2.idp.entityid" idp-entity-id
                    "onelogin.saml2.idp.x509cert" idp-certificate
                    "onelogin.saml2.idp.single_sign_on_service.url" idp-sso-url
                    "onelogin.saml2.sp.entityid" sp-entity-id
                    "onelogin.saml2.sp.assertion_consumer_service.url" sp-acs-url})
      (.build)))

(defn- saml-settings
  "java-saml settings for config. SettingsBuilder drops a value it cannot
  parse, such as a malformed URL or certificate, so checkSettings reports it."
  ^Saml2Settings [config]
  (let [^Saml2Settings settings (build-settings config)
        errors (.checkSettings settings)]
    (when (seq errors)
      (throw (invalid-okta-config (str "Okta config has invalid SAML settings: " (string/join ", " errors)))))
    settings))

(defn- invalid-saml-response
  ([message] (ex-info message {:type ::invalid-saml-response}))
  ([message cause] (ex-info message {:type ::invalid-saml-response} cause)))

(defn- parse-saml-response ^SamlResponse [settings ^String acs-url ^String saml-response]
  (try
    (SamlResponse. settings acs-url saml-response)
    (catch Exception e
      (throw (invalid-saml-response (.getMessage e) e)))))

(defn- validated-response ^SamlResponse [settings acs-url saml-response]
  (let [^SamlResponse response (parse-saml-response settings acs-url saml-response)]
    (when-not (.isValid response nil)
      (let [^Exception cause (.getValidationException response)]
        (throw (invalid-saml-response (.getMessage cause) cause))))
    ;; isValid skips its Audience check when the Assertion names no Audience,
    ;; which would accept a response minted for any service provider.
    (when (empty? (.getAudiences response))
      (throw (invalid-saml-response "SAML Response Assertion has no Audience to match sp/@entityID")))
    response))

(defn- name-id ^String [^SamlResponse response]
  (try
    (.getNameId response)
    (catch Exception e
      (throw (invalid-saml-response (.getMessage e) e)))))

(defn- authenticated-user-email [okta-config saml-response]
  (when (string/blank? saml-response)
    (throw (invalid-saml-response "SAMLResponse parameter is missing or empty")))
  (let [config (parse-okta-config okta-config)
        response (validated-response (saml-settings config) (:sp-acs-url config) saml-response)]
    (string/lower-case (name-id response))))

(defn respond-to-okta-post [okta-config params]
  {:redirect-url (:RelayState params)
   :authenticated-user-email (authenticated-user-email okta-config (:SAMLResponse params))})
