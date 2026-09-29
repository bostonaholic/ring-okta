# ring-okta

[![Build and Test](https://github.com/bostonaholic/ring-okta/actions/workflows/build-and-test.yml/badge.svg?branch=main)](https://github.com/bostonaholic/ring-okta/actions/workflows/build-and-test.yml) [![Clojars Project](https://img.shields.io/clojars/v/dev.bostonaholic/ring-okta.svg)](https://clojars.org/dev.bostonaholic/ring-okta)

Ring middleware for Okta Single Sign-on.

## Installation

### Leiningen/Boot

```clojure
[dev.bostonaholic/ring-okta "1.1.0"]
```

### Clojure CLI/deps.edn

```clojure
dev.bostonaholic/ring-okta {:mvn/version "1.1.0"}
```

### Gradle

```gradle
implementation("dev.bostonaholic:ring-okta:1.1.0")
```

### Maven

```xml
<dependency>
  <groupId>dev.bostonaholic</groupId>
  <artifactId>ring-okta</artifactId>
  <version>1.1.0</version>
</dependency>
```

### Migrating from bostonaholic/ring-okta

In your build file, replace the group `bostonaholic` with `dev.bostonaholic`. The artifact name `ring-okta` stays the same. The namespaces (`ring.middleware.okta` and `ring.ring-okta.*`) and the API do not change, so you do not need to change your code.

The first version under `dev.bostonaholic` is `1.1.0`. No `1.0.x` version exists under the new group. The last release under `bostonaholic/ring-okta` is `1.0.7`, and Clojars still has it.

A dependency can bring in `bostonaholic/ring-okta` transitively, next to `dev.bostonaholic/ring-okta`. Both jars hold the same namespaces. To keep only the new jar, put the lib symbol `bostonaholic/ring-okta` in an `:exclusions` vector on the dependency that brings it in. The `:exclusions [bostonaholic/ring-okta]` form works in both Leiningen and deps.edn. A Leiningen example:

```clojure
[example/lib "1.2.3" :exclusions [bostonaholic/ring-okta]]
```

In Gradle or Maven, exclude group `bostonaholic`, artifact `ring-okta`, from that dependency.

### Okta SAML Toolkit Dependency

`ring-okta` depends on the Okta SAML Toolkit for Java, `com.okta/saml-toolkit` version `1.0.12-000170-c7ed721`, as declared in [project.clj](./project.clj). Okta does not publish this toolkit to a public Maven repository. This repository keeps a copy of the jar in [maven_repository/com/okta/saml-toolkit](./maven_repository/com/okta/saml-toolkit). Download the jar from there. Then install it into your local Maven repository, `~/.m2/repository`, with the `mvn install:install-file` goal.

## Usage

```clojure
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

A build of this project from a clone needs no separate toolkit install. The `"local"` repository in `project.clj` resolves the toolkit jar from `maven_repository/`. To use this library in your own project, use the steps in **Okta SAML Toolkit Dependency** above. The command below installs a new toolkit version into `maven_repository/`, with `-DlocalRepositoryPath` set to that directory:

```shell
mvn install:install-file -Dfile=saml-toolkit.jar -DgroupId=com.okta -DartifactId=saml-toolkit -Dpackaging=jar -Dversion=<version> -DcreateChecksum=true -DupdateReleaseInfo=true -DgeneratePom=true -DlocalRepositoryPath=/path/to/localRepo
```

## Releases

`ring-okta` is released on no particular schedule. New versions are released as needed when features are added or bugs are fixed.

Refer to the [CHANGELOG.md](./CHANGELOG.md) for all version releases and the included changes.

The process for releasing a new version is as follows:

### Pre-steps

1. Bump version in project.clj following [Semantic Versioning 2.0.0](https://semver.org/)
2. Bump version in [README.md](./README.md) to match `project.clj`
3. Add changes to [CHANGELOG.md](./CHANGELOG.md) following [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
4. Generate API docs with `lein codox`

### Release

Nothing is pushed until the Clojars deploy succeeds, so a public tag always has a matching artifact.

1. Commit changes with commit message `Release v<version>`
2. Tag the commit locally with `git tag v<version>`
3. Deploy release to [Clojars](https://clojars.org) with `lein deploy clojars`
4. Push changes to GitHub (including new tag with `--tags` option)
5. Create the GitHub Release:

   ```bash
   gh release create v<version> --title v<version> --verify-tag --notes "**Changelog**: https://github.com/bostonaholic/ring-okta/blob/main/CHANGELOG.md#<changelog-anchor>

   **Full Changelog**: https://github.com/bostonaholic/ring-okta/compare/v<previous-version>...v<version>"
   ```

   - `<previous-version>` is the version released before this one (e.g. `1.0.7`).
   - `<changelog-anchor>` is GitHub's anchor for the version's `CHANGELOG.md` heading: lowercase it, drop the brackets and dots, and replace each space with `-`. The heading `## [1.1.0] - 2026-09-29` becomes `110---2026-09-29`.
   - `--verify-tag` makes the command fail if the tag was not pushed, instead of creating a new tag.

### Post-steps

1. Bump patch version of `project.clj` to next `-SNAPSHOT`
2. Commit snapshot version with commit message `<version>`

## License

Copyright © 2025 Matthew Boston

Released under the MIT License.
