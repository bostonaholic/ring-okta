(ns ring.middleware.okta
  "Ring middleware for Okta Single Sign-on"
  (:require [clojure.java.io :as io]
            [compojure.core :refer [POST defroutes]]
            [ring.ring-okta.predicates :as p]
            [ring.ring-okta.session :as okta-session]
            [ring.util.response :as ring-response]))

(defroutes okta-routes
  "The compojure routes for Okta

   POST /login
   POST /logout"

  {:added "0.1.0"}

  (POST "/login" request
        (okta-session/login request))

  (POST "/logout" request
        (okta-session/logout request)
        (merge (ring-response/redirect-after-post (:redirect-after-logout request)))))

(defn wrap-okta
  "Ring middleware for Okta Single Sign-on

  Arguments:

  handler                - the ring handler function

  okta-home              - the URL to be redirected to for Okta login
                           e.g. https://company.okta.com

  Accepts the following options:

  :okta-config           - the location of the Okta configuration file
                           (defaults to \"resources/okta-config.xml\")

  :redirect-after-logout - the destination URL to be redirected to after a `POST /logout`
                           (defaults to \"/\")

  :skip-routes           - a list of routes as a string or regex to skip Okta authentication
                           e.g. [:get \"/about\" :any \"/contact\" :get \"/home/\\S+\"]

  :force-user            - a default user to be used for development"

  {:arglists '([handler okta-home] [handler okta-home options]) :added "0.1.0"}

  ([handler okta-home] (wrap-okta handler okta-home {}))

  ([handler okta-home {:keys [okta-config redirect-after-logout]
                       :or {okta-config (io/resource "okta-config.xml")
                            redirect-after-logout "/"}
                       :as options}]

   {:pre [(not-empty okta-home)]}

   (fn [request]
     (cond
       (p/login? request) (handler (assoc request :okta-config-location okta-config))
       (p/logout? request) (handler (assoc request :redirect-after-logout redirect-after-logout))
       (p/logged-in? request) (handler request)
       (p/force-user? (:force-user options)) (handler (assoc-in request [:session :okta/user] (:force-user options)))
       (p/skip-route? request (:skip-routes options)) (handler request)
       :else (ring-response/redirect okta-home)))))
