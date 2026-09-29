(ns example-app.core
  (:require [compojure.core :as compojure :refer [defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.okta :refer [wrap-okta okta-routes]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]))

(defroutes company-routes
  (compojure/GET "/" [] "<h1>Hello World</h1>")

  okta-routes

  (route/not-found "<h1>Page not found</h1>"))

;; wrap-okta reads the SAML POST from keyword :params and the logged-in
;; user from :session, so those middlewares must run before it.
(def app
  (-> company-routes
      (wrap-okta "https://example.okta.com" {:okta-config "resources/custom-okta-config.xml"})
      wrap-keyword-params
      wrap-params
      wrap-session))

(defn start-server [port]
  (jetty/run-jetty app {:port port
                        :join? false}))

(defn -main [& _]
  (start-server 3000))
