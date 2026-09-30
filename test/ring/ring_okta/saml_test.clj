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
             (rejection-code okta-config (fixture "N1")))))

    (testing "rejects forged responses"
      (testing "signed by another key"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/INVALID_SIGNATURE}
               (rejection-code okta-config (fixture "N2")))))
      (testing "unsigned"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/NO_SIGNATURE_FOUND}
               (rejection-code okta-config (fixture "N6")))))
      (testing "with an unsigned assertion before the signed one"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_NUMBER_OF_ASSERTIONS}
               (rejection-code okta-config (fixture "N7a"))))
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_NUMBER_OF_ASSERTIONS}
               (rejection-code okta-config (fixture "N7b")))))
      (testing "issued by another Okta app"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_ISSUER}
               (rejection-code okta-config (fixture "N10"))))))

    (testing "rejects unusable responses"
      (testing "with a non-Success status"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/STATUS_CODE_IS_NOT_SUCCESS}
               (rejection-code okta-config (fixture "N8")))))
      (testing "with a DOCTYPE"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/INVALID_XML_FORMAT}
               (rejection-code okta-config (fixture "N9")))))
      (testing "that is not base64"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/INVALID_XML_FORMAT}
               (rejection-code okta-config "not base64!"))))
      (testing "that is base64 of text that is not XML"
        ;; "aGVsbG8=" is base64 of "hello"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/INVALID_XML_FORMAT}
               (rejection-code okta-config "aGVsbG8="))))
      (testing "with no NameID"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/NO_NAMEID}
               (rejection-code okta-config (fixture "N11"))))))

    (testing "rejects a missing SAMLResponse"
      (testing "nil"
        (is (= {:type ::saml/invalid-saml-response
                :code nil
                :message "SAMLResponse parameter is missing or empty"
                :cause nil}
               (rejection okta-config {:SAMLResponse nil :RelayState "/dashboard"}))))
      (testing "empty"
        (is (= {:type ::saml/invalid-saml-response
                :code nil
                :message "SAMLResponse parameter is missing or empty"
                :cause nil}
               (rejection okta-config {:SAMLResponse "" :RelayState "/dashboard"})))))

    (testing "rejects responses for another service provider"
      (testing "wrong Audience"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_AUDIENCE}
               (rejection-code okta-config (fixture "N4")))))
      (testing "wrong Destination"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_DESTINATION}
               (rejection-code okta-config (fixture "N5")))))
      (testing "wrong SubjectConfirmationData Recipient"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_SUBJECTCONFIRMATION}
               (rejection-code okta-config (fixture "N5b")))))
      (testing "no Audience"
        (let [rejected (rejection okta-config {:SAMLResponse (fixture "N4b") :RelayState "/dashboard"})]
          (is (= {:type ::saml/invalid-saml-response :cause nil}
                 (select-keys rejected [:type :cause])))
          (is (re-find #"Audience" (str (:message rejected)))))))

    (testing "rejects responses outside their time window"
      (testing "expired"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/ASSERTION_EXPIRED}
               (rejection-code okta-config (fixture "N3")))))
      (testing "not yet valid"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/ASSERTION_TOO_EARLY}
               (rejection-code okta-config (fixture "N3b")))))
      (testing "SubjectConfirmationData expired"
        (is (= {:type ::saml/invalid-saml-response :code ValidationError/WRONG_SUBJECTCONFIRMATION}
               (rejection-code okta-config (fixture "N3c"))))))

    (testing "accepts each supported Okta signing setting"
      (testing "assertion signed, response unsigned"
        (is (= "jane.doe@example.com"
               (:authenticated-user-email
                (respond-to-okta-post okta-config {:SAMLResponse (fixture "V2") :RelayState "/dashboard"})))))
      (testing "response signed, assertion unsigned"
        (is (= "jane.doe@example.com"
               (:authenticated-user-email
                (respond-to-okta-post okta-config {:SAMLResponse (fixture "V3") :RelayState "/dashboard"})))))
      (testing "SHA-1 digest under an RSA-SHA256 signature"
        (is (= "jane.doe@example.com"
               (:authenticated-user-email
                (respond-to-okta-post okta-config {:SAMLResponse (fixture "V5") :RelayState "/dashboard"}))))))

    (testing "rejects RSA-SHA1 signatures"
      (is (= {:type ::saml/invalid-saml-response :code ValidationError/INVALID_SIGNATURE}
             (rejection-code okta-config (fixture "V4")))))

    (testing "accepts a 1.x config with <sp> added"
      (is (config-has? okta-config "/configuration/default"))
      (is (config-has? okta-config "/configuration/loginUri"))
      (is (config-has? okta-config "/configuration/suppressErrors"))
      (is (config-has? okta-config "/configuration/allowedAddresses"))
      (is (config-has? okta-config "/configuration/allowedAddresses/oktaUsers"))
      (is (config-has? okta-config "/configuration/allowedAddresses/spUsers"))
      (is (config-has? okta-config "/configuration/allowedAddresses/spGroups"))
      (is (= "jane.doe@example.com"
             (:authenticated-user-email
              (respond-to-okta-post okta-config {:SAMLResponse (fixture "V1") :RelayState "/dashboard"})))))

    (testing "rejects an invalid Okta config and names the fix"
      (let [params {:SAMLResponse (fixture "V1") :RelayState "/dashboard"}]
        (testing "0-byte config"
          (is (= ::saml/invalid-okta-config
                 (:type (rejection (slurp (io/resource "custom-okta-config.xml")) params)))))
        (testing "missing <sp>"
          (let [rejected (rejection (config "missing-sp") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"sp/@entityID" (str (:message rejected))))))
        (testing "missing sp/@assertionConsumerServiceURL"
          (let [rejected (rejection (config "missing-acs-url") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"sp/@assertionConsumerServiceURL" (str (:message rejected))))))
        (testing "missing md:EntityDescriptor/@entityID"
          (let [rejected (rejection (config "missing-idp-entity-id") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"md:EntityDescriptor/@entityID" (str (:message rejected))))))
        (testing "missing HTTP-POST SingleSignOnService @Location"
          (let [rejected (rejection (config "missing-sso-location") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"md:EntityDescriptor/md:IDPSSODescriptor/md:SingleSignOnService\[@Binding='urn:oasis:names:tc:SAML:2\.0:bindings:HTTP-POST'\]/@Location"
                         (str (:message rejected))))))
        (testing "zero <application> elements"
          (let [rejected (rejection (config "zero-applications") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"\b0\b" (str (:message rejected))))))
        (testing "two <application> elements"
          (let [rejected (rejection (config "two-applications") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"\b2\b" (str (:message rejected))))))
        (testing "zero signing certificates"
          (let [rejected (rejection (config "zero-certificates") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"\b0\b" (str (:message rejected))))))
        (testing "two signing certificates"
          (let [rejected (rejection (config "two-certificates") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"\b2\b" (str (:message rejected))))))
        (testing "DOCTYPE in the config"
          (is (= ::saml/invalid-okta-config
                 (:type (rejection (config "doctype") params)))))
        (testing "malformed ACS URL"
          (let [rejected (rejection (config "malformed-acs-url") params)]
            (is (= ::saml/invalid-okta-config (:type rejected)))
            (is (re-find #"sp_acs_not_found" (str (:message rejected))))))))))
