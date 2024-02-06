# ring-okta

[![Build and Test](https://github.com/bostonaholic/ring-okta/actions/workflows/build-and-test.yml/badge.svg?branch=main)](https://github.com/bostonaholic/ring-okta/actions/workflows/build-and-test.yml) [![Clojars Project](https://img.shields.io/clojars/v/bostonaholic/ring-okta.svg)](https://clojars.org/bostonaholic/ring-okta)

Ring middleware for Okta Single Sign-on.

## Installation

### Leiningen/Boot

```
[bostonaholic/ring-okta "1.0.6"]
```

### Clojure CLI/deps.edn

```
bostonaholic/ring-okta {:mvn/version "1.0.6"}
```

### Gradle

```
implementation("bostonaholic:ring-okta:1.0.6")
```

### Maven

```
<dependency>
  <groupId>bostonaholic</groupId>
  <artifactId>ring-okta</artifactId>
  <version>1.0.6</version>
</dependency>
```

### Okta SAML Toolkit Dependency

Since Okta doesn't publish the SAML Toolkit for Java, you must download it [here](https://support.okta.com/entries/25009573-Current-SAML-Toolkit-for-Java-Version). You then must `mvn install` it to your local maven repository. Check the [project.clj](./project.clj) for the version of the SAML Toolkit to download from Okta.

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

The documentation is built with [codox](https://github.com/weavejester/codox) (`lein codox`) and published to `./docs` which ends up being hosted by GitHub Pages.

## Test Coverage

The test coverage summary is built with [cloverage](https://github.com/lshift/cloverage) (`lein cloverage`) and published to `./docs/coverage` and is hosted [here](https://bostonaholic.github.io/ring-okta/coverage/index.html).

## Development

As described in **Usage** above, the Okta SAML Toolkit must be downloaded and installed to your local maven repository. When updating this dependency, here is how you can install the downloaded jar:

```
$ mvn install:install-file -Dfile=saml-toolkit.jar -DgroupId=com.okta -DartifactId=saml-toolkit -Dpackaging=jar -Dversion=<version> -DcreateChecksum=true -DupdateReleaseInfo=true -DgeneratePom=true -DlocalRepositoryPath=/path/to/localRepo
```

## Releases

`ring-okta` is released on no particular schedule. New versions are released as needed when features are added or bugs are fixed.

Refer to the [CHANGELOG.md](./CHANGELOG.md) for all version releases and the included changes.

The process for releasing a new version is as follows:

#### Pre-steps

1. Bump version in project.clj following [Semantic Versioning 2.0.0](https://semver.org/)
2. Bump version in [README.md](./README.md) to match `project.clj`
3. Add changes to [CHANGELOG.md](./CHANGELOG.md) following [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
3. Generate API docs with `lein codox`

#### Release

1. Commit changes with commit message `Release v<version>`
2. Tag the commit with `git tag v<version>`
3. Push changes to GitHub (including new tag with `--tags` option)
4. Deploy release to [Clojars](https://clojars.org) with `lein deploy clojars`

#### Post-steps

1. Bump patch version of `project.clj` to next `-SNAPSHOT`
2. Commit snapshot version with commit message `<version>`

## License

Copyright © 2024 Matthew Boston

Released under the MIT License.
