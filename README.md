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

### SAML Dependency

`ring-okta` validates Okta SAML responses with [java-saml-core](https://github.com/onelogin/java-saml) `2.9.0` (`com.onelogin/java-saml-core`, MIT License). Your build tool gets it from Maven Central with the other dependencies. You do not need a manual install.

java-saml writes the full SAML response to its log at DEBUG level. Keep the `com.onelogin` logger above DEBUG in production, because anyone who can read those log lines can replay the response until it expires.

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

### Okta Configuration

This section applies from version `2.0.0`. If you use `1.x`, refer to the [CHANGELOG.md](./CHANGELOG.md).

`wrap-okta` reads the Okta configuration file from the `:okta-config` option, or from `okta-config.xml` on the classpath. The file holds one `application`. Its `md:EntityDescriptor` is the IdP metadata from your Okta app, unchanged. The `sp` element identifies your app. Copy its values from these Okta app fields:

- `entityID`: "Audience URI (SP Entity ID)".
- `assertionConsumerServiceURL`: "Single sign-on URL". This is the URL of your `POST /login` route.

```xml
<configuration><applications><application>
  <md:EntityDescriptor entityID="http://www.okta.com/exk...">...unchanged Okta IdP metadata...</md:EntityDescriptor>
  <sp entityID="https://app.example.com/" assertionConsumerServiceURL="https://app.example.com/login"/>
</application></applications></configuration>
```

The tests cover these Okta signing settings:

| Okta "Response" and "Assertion Signature" | Signature algorithm | Digest algorithm | Result |
|---|---|---|---|
| Both signed | RSA-SHA256 | SHA256 | Accepted |
| Assertion signed only | RSA-SHA256 | SHA256 | Accepted |
| Response signed only | RSA-SHA256 | SHA256 | Accepted |
| Both signed | RSA-SHA256 | SHA1 | Accepted |
| Both signed | RSA-SHA1 | SHA1 | Rejected. Set the Okta app to RSA-SHA256. |

When a login fails, `ring-okta` throws an `ExceptionInfo`. `(:type (ex-data e))` is `:ring.ring-okta.saml/invalid-saml-response` for a response that fails validation. Your app decides the HTTP status.

The tests use signed responses in the format that Okta documents. They do not come from a live Okta tenant. If a real Okta response fails to validate, [open an issue](https://github.com/bostonaholic/ring-okta/issues).

## Documentation

- [API Docs](http://bostonaholic.github.io/ring-okta/index.html)

The documentation is built with [codox](https://github.com/weavejester/codox) (`lein codox`) and published to `./docs` which ends up being hosted by GitHub Pages.

## Test Coverage

The test coverage summary is built with [cloverage](https://github.com/lshift/cloverage) (`lein cloverage`) and published to `./docs/coverage` and is hosted [here](https://bostonaholic.github.io/ring-okta/coverage/index.html).

## Development

The tests validate signed SAML responses in `test-resources/saml/` against `test-resources/okta-config.xml`. To regenerate them, run the command below with `JAVA_HOME` set to JDK 17 or later:

```shell
JAVA_HOME=/path/to/jdk17+ script/generate-saml-fixtures
```

Each run makes new throwaway keys, so it rewrites `okta-config.xml` and every `.b64` file. Commit them together. The script deletes the keys when it exits. Never commit a private key or keystore.

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

Copyright © 2026 Matthew Boston

Released under the MIT License.
