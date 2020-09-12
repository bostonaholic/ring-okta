(ns ring.ring-okta.saml-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest testing is]]
            [ring.ring-okta.saml :refer [respond-to-okta-post]]))

(deftest test-respond-to-okta-post
  (with-redefs [ring.ring-okta.saml/get-valid-user-id (fn [& args] "foo@bar.com")]
    (let [params {:RelayState "http://foo.bar.com"}
          okta-config (slurp (io/resource "okta-config.xml"))
          response (respond-to-okta-post okta-config params)]

      (testing ":redirect-url is the :RelayState parameter from Okta"
        (is (= "http://foo.bar.com" (:redirect-url response))))

      (testing ":authenticated-user-email is decoded from the SAML Response"
        (is (= "foo@bar.com" (:authenticated-user-email response)))))))
