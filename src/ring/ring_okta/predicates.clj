(ns ring.ring-okta.predicates
  (:require [ring.util.request :as ring-request]
            [clojure.string :as str]))

(def not-nil? (comp not nil?))

(defn login? [{:keys [request-method] :as request}]
  (and (= request-method :post)
       (= (ring-request/path-info request) "/login")))

(defn logout? [{:keys [request-method] :as request}]
  (and (= request-method :post)
       (= (ring-request/path-info request) "/logout")))

(defn logged-in? [{:keys [session]}]
  (:okta/user session))

(defn- match-pair? [[skip-method skip-path] request-method request-path]
  (and (or (= :any skip-method)
           (= skip-method request-method))
       (str/starts-with? request-path skip-path)))

(defn skip-route? [{:keys [request-method] :as request} skip-routes]
  (when skip-routes
    (let [request-path (ring-request/path-info request)
          skip-pairs (partition 2 skip-routes)]
      (some #(match-pair? % request-method request-path) skip-pairs))))

(defn force-user? [force-user]
  (not-nil? force-user))
