(ns ring.ring-okta.session-test
  (:require [clojure.test :refer [deftest testing is]]
            [ring.ring-okta.saml :as saml]
            [ring.ring-okta.session :as session]))

(defn- stub-respond-to-okta-post [& _]
  {:redirect-url "http://foo.bar.com"
   :authenticated-user-email "foo@bar.com"})

(deftest test-login
  (let [request {:params {} :okta-config-location "test-resources/okta-config.xml"}]
    (with-redefs [saml/respond-to-okta-post stub-respond-to-okta-post]
      (testing "user placed in session"
        (is (= "foo@bar.com" (-> (session/login request) :session :okta/user))))

      (testing "redirect after login"
        (is (= 303 (-> (session/login request) :status)))
        (is (= "http://foo.bar.com" (-> (session/login request) :headers (get "Location"))))))))

(deftest test-logout
  (let [request {:params {:foo "foo"}
                 :session {:okta/user "foo@bar.com"
                           :bar "bar"}}]
    (testing "logout removes only :okta/user from session"
      (is (= {:bar "bar"} (-> (session/logout request) :session))))

    (testing "logout does not clear other items in the request"
      (is (= {:params {:foo "foo"} :session {:bar "bar"}} (session/logout request))))))
