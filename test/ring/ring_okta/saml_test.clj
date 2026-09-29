(ns ring.ring-okta.saml-test
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [ring.ring-okta.saml :as saml :refer [respond-to-okta-post]])
  (:import (com.onelogin.saml2.exception ValidationError)
           (java.io ByteArrayInputStream)
           (javax.xml.parsers DocumentBuilderFactory)
           (javax.xml.xpath XPathConstants XPathFactory)))

(defn- fixture
  "The base64 SAMLResponse in test-resources/saml/<id>.b64, as Okta posts it.
  script/generate-saml-fixtures writes these files."
  [id]
  (let [path (str "saml/" id ".b64")]
    (string/trim (slurp (or (io/resource path)
                            (throw (ex-info (str "Missing fixture " path ", run script/generate-saml-fixtures")
                                            {:path path})))))))

(defn- config
  "The hand-written Okta config test-resources/saml/config/<config-name>.xml."
  [config-name]
  (slurp (io/resource (str "saml/config/" config-name ".xml"))))

(defn- rejection
  "Calls respond-to-okta-post and describes the exception it throws, or
  returns nil when nothing throws. :code is the ValidationError code of the
  cause, :cause the class of the cause."
  [okta-config params]
  (try
    (respond-to-okta-post okta-config params)
    nil
    (catch Exception e
      (let [cause (.getCause e)]
        {:type (:type (ex-data e))
         :code (when (instance? ValidationError cause)
                 (.getErrorCode ^ValidationError cause))
         :message (.getMessage e)
         :cause (some-> cause class)}))))

(defn- rejection-code
  "The :type and ValidationError :code of the rejection of saml-response,
  or nil when nothing throws."
  [okta-config saml-response]
  (some-> (rejection okta-config {:SAMLResponse saml-response :RelayState "/dashboard"})
          (select-keys [:type :code])))

(defn- config-has?
  "True when the XPath expression selects a node in the config XML."
  [config-xml ^String xpath-expr]
  (let [builder (.newDocumentBuilder (DocumentBuilderFactory/newInstance))
        doc (.parse builder (ByteArrayInputStream. (.getBytes ^String config-xml "UTF-8")))
        xpath (.newXPath (XPathFactory/newInstance))]
    (.evaluate xpath xpath-expr doc XPathConstants/BOOLEAN)))

(deftest test-respond-to-okta-post
  (let [okta-config (slurp (io/resource "okta-config.xml"))]

    ;; Slice 1
    (testing "accepts a current-format Okta response"
      (let [params {:SAMLResponse (fixture "V1") :RelayState "/dashboard"}
            expected {:redirect-url "/dashboard"
                      :authenticated-user-email "jane.doe@example.com"}]
        (is (= expected (respond-to-okta-post okta-config params)))
        (is (= expected (respond-to-okta-post okta-config params)))
        (let [futures (doall (repeatedly 8 #(future (respond-to-okta-post okta-config params))))]
          (is (= (repeat 8 expected) (mapv deref futures))))))

    (testing "rejects a tampered response"
      (is (= {:type ::saml/invalid-saml-response :code ValidationError/INVALID_SIGNATURE}
             (rejection-code okta-config (fixture "N1")))))))
