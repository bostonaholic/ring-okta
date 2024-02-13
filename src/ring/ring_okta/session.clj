(ns ring.ring-okta.session
  (:require [clojure.core.incubator :refer [dissoc-in]]
            [clojure.java.io :as io]
            [ring.ring-okta.saml :as saml]
            [ring.util.response :as response]))

(defn login [{:keys [okta-config-location params]}]
  (let [okta-config-contents (with-open [r (io/reader okta-config-location)] (slurp r))
        okta-response (saml/respond-to-okta-post okta-config-contents params)]
    (assoc-in
     (response/redirect (:redirect-url okta-response) (response/redirect-status-codes :see-other))
     [:session :okta/user]
     (:authenticated-user-email okta-response))))

(defn logout [request]
  (dissoc-in request [:session :okta/user]))
