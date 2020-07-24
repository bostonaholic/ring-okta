# ring-okta

[![CircleCI](https://circleci.com/gh/bostonaholic/ring-okta/tree/master.svg?style=svg)](https://circleci.com/gh/bostonaholic/ring-okta/tree/master)

Ring middleware for Okta Single Sign-on.

## Installation

### Leiningen

```
[bostonaholic/ring-okta "0.3.1"]
```

### Gradle

```
compile "bostonaholic:ring-okta:0.3.1"
```

### Maven

```
<dependency>
  <groupId>bostonaholic</groupId>
  <artifactId>ring-okta</artifactId>
  <version>0.3.1</version>
</dependency
```

### Okta SAML Toolkit Dependency

Since Okta doesn't publish the SAML Toolkit for Java, you must
download it
[here](https://support.okta.com/entries/25009573-Current-SAML-Toolkit-for-Java-Version).
You then must `mvn install` it to your local maven repository. Check
the `project.clj` for the version of the SAML Toolkit to download from Okta.

## Usage

```
(ns com.company.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.okta :refer [wrap-okta okta-routes]]))

(defroutes company-routes
  (GET "/" [] "<h1>Hello World</h1>")

  okta-routes

  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> company-routes
      (wrap-okta "https://company.okta.com")))
```

## Documentation

- [API Docs](http://bostonaholic.github.io/ring-okta/index.html)

The documentation is built with [codox](https://github.com/weavejester/codox) (`lein codox`) and published to `/.docs` which ends up being hosted by GitHub Pages.

## Test Coverage

The test coverage summary is built with [cloverage](https://github.com/lshift/cloverage) (`lein cloverage`) and hosted [here](https://bostonaholic.github.io/ring-okta/coverage/index.html).

## Development

As described in **Usage** above, the Okta SAML Toolkit must be downloaded and installed to your local maven repository. When updating this dependency, here is how you can install the downloaded jar:

```
$ mvn install:install-file -Dfile=saml-toolkit.jar -DgroupId=com.okta -DartifactId=saml-toolkit -Dpackaging=jar -Dversion=<version> -DcreateChecksum=true -DupdateReleaseInfo=true -DgeneratePom=true -DlocalRepositoryPath=/path/to/localRepo
```

## License

Copyright © 2020 Matthew Boston

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
