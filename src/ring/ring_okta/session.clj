(ns ring.ring-okta.session
  (:require [clojure.core.incubator :refer [dissoc-in]]
            [clojure.java.io :as io]
            [ring.ring-okta.saml :as saml]
            [ring.util.response :as response]))

(defn login [{:keys [okta-config-location params]}]
  (let [okta-response (saml/respond-to-okta-post (with-open [r (io/reader okta-config-location)] (slurp r)) params)]
    (assoc-in
     (response/redirect-after-post (:redirect-url okta-response))
     [:session :okta/user]
     (:authenticated-user-email okta-response))))

(defn logout [request]
  (dissoc-in request [:session :okta/user]))
