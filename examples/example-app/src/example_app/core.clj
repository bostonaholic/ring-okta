(ns example-app.core
  (:require [compojure.core :as compojure :refer [defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.okta :refer [wrap-okta okta-routes]]))

(defroutes company-routes
  (compojure/GET "/" [] "<h1>Hello World</h1>")

  okta-routes

  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> company-routes
      (wrap-okta "https://company.okta.com" {:okta-config "resources/custom-okta-config.xml"})))

(defn start-server [port]
  (jetty/run-jetty app {:port port
                        :join? false}))

(defn -main [& _]
  (start-server 3000))
