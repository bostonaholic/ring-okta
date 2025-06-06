(defproject bostonaholic/ring-okta "1.0.8-SNAPSHOT"
  :description "Ring middleware for Okta Single Sign-on"
  :url "https://github.com/bostonaholic/ring-okta"
  :license {:name "The MIT License (MIT)"
            :url "https://mit-license.org"}
  :repositories [["local" ~(str (.toURI (java.io.File. "maven_repository")))]]
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/core.incubator "0.1.4"]
                 [ring/ring-core "1.14.1" :scope "provided" :exclusions [commons-codec]]
                 [compojure "1.7.1" :exclusions [org.clojure/clojure ring/ring-codec commons-codec joda-time]]
                 [org.clojure/data.codec "0.2.0"]
                 [com.okta/saml-toolkit "1.0.12-000170-c7ed721" :upgrade :okta]

                 ;; okta dependencies -- some are not specified in their pom,
                 ;; others are borked because of our weird local repo thing that
                 ;; we do in order to please the Travis-CI gods
                 ;; FIXME: check if local repo is needed on GitHub Actions
                 [com.sun.xml.parsers/jaxp-ri "1.4.5" :upgrade :okta]
                 [org.slf4j/slf4j-api "1.6.1" :scope "provided" :upgrade :okta]
                 [org.slf4j/slf4j-simple "1.6.1" :scope "test" :upgrade :okta]
                 [com.google.inject/guice "3.0" :upgrade :okta]
                 [org.bouncycastle/bcprov-jdk16 "1.45" :upgrade :okta]
                 [org.apache.commons/commons-lang3 "3.0" :upgrade :okta]
                 [javax.servlet/javax.servlet-api "3.0.1" :scope "provided" :upgrade :okta]
                 [org.opensaml/opensaml "2.6.4" :upgrade :okta]]

  :pedantic? :abort

  :plugins [[lein-ancient "0.7.0"]
            [lein-codox "0.10.8"]
            [lein-cloverage "1.2.4"]]

  :codox {:namespaces [ring.middleware.okta]
          :output-path "./docs"
          :source-uri "https://github.com/bostonaholic/ring-okta/blob/v{version}/{filepath}#L{line}"}

  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[ring-mock "0.1.5"]]}
             :1.10 {:resource-paths ["test-resources"]
                    :dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:resource-paths ["test-resources"]
                    :dependencies [[org.clojure/clojure "1.11.3"]]}
             :1.12 {:resource-paths ["test-resources"]
                    :dependencies [[org.clojure/clojure "1.12.1"]]}}

  :aliases {"deps-all" ["with-profile" "dev:dev,1.10:dev,1.11:dev,1.12" "deps"]
            "test-all" ["with-profile" "dev:dev,1.10:dev,1.11:dev,1.12" "test"]
            "cloverage" ["do" "cloverage" "--output" "docs/coverage"]
            "release" ["do" "clean," "deploy" "clojars"]})
