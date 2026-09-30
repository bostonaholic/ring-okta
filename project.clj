(defproject dev.bostonaholic/ring-okta "2.0.1-SNAPSHOT"
  :description "Ring middleware for Okta Single Sign-on"
  :url "https://github.com/bostonaholic/ring-okta"
  :license {:name "The MIT License (MIT)"
            :url "https://mit-license.org"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/core.incubator "0.1.4"]
                 [ring/ring-core "1.15.2" :scope "provided" :exclusions [commons-codec]]
                 [compojure "1.7.2" :exclusions [org.clojure/clojure ring/ring-codec commons-codec joda-time]]
                 [com.onelogin/java-saml-core "2.9.0"]

                 ;; xmlsec and commons-lang3 override the versions java-saml-core
                 ;; brings in, which carry CVE-2023-44483 and CVE-2025-48924.
                 ;; :pedantic? :abort then demands the slf4j-api and
                 ;; commons-codec pins.
                 [org.apache.santuario/xmlsec "2.3.5"]
                 [org.apache.commons/commons-lang3 "3.18.0"]
                 [org.slf4j/slf4j-api "1.7.36"]
                 [commons-codec "1.17.1"]
                 [org.slf4j/slf4j-simple "1.7.36" :scope "test"]]

  :pedantic? :abort

  :plugins [[lein-ancient "0.7.0"]
            [lein-codox "0.10.8"]
            [lein-cloverage "1.2.4"]]

  ;; :jquery3 overrides the default theme's bundled jQuery 1.11.0 (vulnerable)
  ;; with 3.6.4. The theme lives in codox-theme/, which only the :codox profile
  ;; (merged by lein-codox itself) puts on the classpath, so it never ships in the jar.
  :codox {:themes [:default :jquery3]
          :namespaces [ring.middleware.okta]
          :output-path "./docs"
          :source-uri "https://github.com/bostonaholic/ring-okta/blob/v{version}/{filepath}#L{line}"}

  :profiles {:codox {:resource-paths ["codox-theme"]}
             :dev {:resource-paths ["test-resources"]
                   :dependencies [[ring-mock "0.1.5"]]}
             :1.10 {:resource-paths ["test-resources"]
                    :dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:resource-paths ["test-resources"]
                    :dependencies [[org.clojure/clojure "1.11.3"]]}
             :1.12 {:resource-paths ["test-resources"]
                    :dependencies [[org.clojure/clojure "1.12.2"]]}}

  :aliases {"deps-all" ["with-profile" "dev:dev,1.10:dev,1.11:dev,1.12" "deps"]
            "test-all" ["with-profile" "dev:dev,1.10:dev,1.11:dev,1.12" "test"]
            "cloverage" ["do" "cloverage" "--output" "docs/coverage"]
            "release" ["do" "clean," "deploy" "clojars"]})
