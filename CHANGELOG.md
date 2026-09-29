<!-- markdownlint-configure-file {
  "no-duplicate-heading": {
    "siblings_only": true
  }
} -->

# Changelog

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/) and [semver.org](https://semver.org/spec/v2.0.0.html).

- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.

## [Unreleased]

Version `2.0.0` breaks existing configurations. Each Okta configuration file needs a new `sp` element. Refer to **Okta Configuration** in the [README](./README.md).

If you stay on `1.x` with JDK 16 or later, every login throws `IllegalAccessError` unless you start the JVM with `--add-exports=java.xml/com.sun.org.apache.xpath.internal.jaxp=ALL-UNNAMED`. The `1.x` toolkit jar is at [maven_repository/com/okta/saml-toolkit](https://github.com/bostonaholic/ring-okta/tree/v1.1.0/maven_repository/com/okta/saml-toolkit) in tag `v1.1.0`.

### Changed

- Replace the Okta SAML Toolkit `1.0.12-000170-c7ed721` with java-saml-core `2.9.0` from Maven Central. You do not need a manual install.
- Each Okta configuration file needs an `sp` element with `entityID` and `assertionConsumerServiceURL`.
- A SAML response that fails validation throws `ExceptionInfo` with `:type` `:ring.ring-okta.saml/invalid-saml-response`, not an OpenSAML or toolkit exception. The java-saml exception is the cause.
- Reject a SAML response signed with RSA-SHA1. Set the Okta app to RSA-SHA256 before you upgrade.
- Responses with both the response and the assertion signed, or with only the response signed, keep the `1.x` result.
- Bump the major version to `2.0.0`.

### Removed

- Remove `maven_repository/` and the toolkit jar.
- Remove the dependencies `com.okta/saml-toolkit`, `org.opensaml/opensaml`, `org.bouncycastle/bcprov-jdk16`, `com.google.inject/guice`, `com.sun.xml.parsers/jaxp-ri`, `javax.servlet/javax.servlet-api`, and `org.clojure/data.codec`. Code that used them through `ring-okta` must declare them.

### Fixed

- Fix the `IllegalAccessError` that failed every login on JDK 16 and later.
- Accept a SAML response that Okta signs on the assertion only. `1.x` rejected it.

### Security

- Reject a SAML response for another service provider: a wrong Audience, a wrong Destination, a wrong `SubjectConfirmationData` Recipient, or no Audience at all. `1.1.0` accepted a wrong Audience and a wrong Destination.
- Reject RSA-SHA1 signatures. `1.1.0` accepted them.
- Pin `org.apache.santuario/xmlsec` `2.3.5` (CVE-2023-44483) and `org.apache.commons/commons-lang3` `3.18.0` (CVE-2025-48924) over the versions that java-saml-core brings in.
- Remove `opensaml` `2.6.4` (CVE-2015-1796, no support since 2016), `bcprov-jdk16` `1.45`, and `commons-lang3` `3.0`, which have known advisories.

## [1.1.0] - 2026-09-29

### Changed

- Publish as `dev.bostonaholic/ring-okta`; `1.0.7` is the last release as `bostonaholic/ring-okta`.
- Move ring-mock to dev dependency.
- Replace deprecated function `redirect-after-post`.
- Upgrade ring-core 1.15.2.
- Upgrade compojure 1.7.2.
- Upgrade org.clojure/data.codec 0.2.0.

### Added

- Added support for clojure 1.12.1.

## [1.0.7] - 2024-02-06

### Changed

- Update minimum clojure version to 1.9.0.
- Upgrade clojure 1.11.1.
- Upgrade ring-core 1.11.0.
- Upgrade compojure 1.7.1.

## [1.0.6] - 2023-06-15

### Changed

- Upgrade lein-cloverage 1.2.4.
- Upgrade compojure 1.7.0.
- Upgrade ring-core 1.9.6.

## [1.0.5] - 2022-05-26

### Changed

- Upgrade clojure 1.10.3.
- Upgrade lein-codox 0.10.8.
- Remove unnecessary dependency on cloverage in dev profile.
- Upgrade ring-core 1.9.5.
- Upgrade lein-cloverage 1.2.3.
- Upgrade compojure 1.6.3.

## [1.0.4] - 2021-07-30

### Changed

- Upgrade ring-core 1.9.4.

## [1.0.3] - 2021-05-21

### Changed

- Upgrade ring-core 1.9.3.

## [1.0.2] - 2021-04-20

### Changed

- Upgrade ring-core 1.9.2.

## [1.0.1] - 2021-02-19

### Changed

- Upgrade cloverage 1.2.2.
- Upgrade lein-ancient 0.7.0.
- Upgrade ring-core 1.9.1.
- Upgrade clojure 1.10.2.

## [1.0.0] - 2020-10-24

### Changed

- Released under the MIT License.

## [0.5.1] - 2020-10-24

### Changed

- Upgrade ring-core 1.8.2.
- Upgrade cloverage 1.2.1.

## [0.5.0] - 2020-09-13

### Fixed

- Allow `:okta-config` to be present outside of bundled jar, [\#4](https://github.com/bostonaholic/ring-okta/pull/4) by [@ravik-karn-tw](https://github.com/ravik-karn-tw).

## [0.4.0] - 2020-09-05

### Added

- Support for regexes in `:skip-routes`, [\#2](https://github.com/bostonaholic/ring-okta/pull/2) by [@sudhinm](https://github.com/sudhinm).

## [0.3.2] - 2020-08-19

### Changed

- Upgrade ring-core 1.8.1.
- Upgrade compojure 1.6.2.
- Upgrade cloverage 1.2.0.
- Upgrade lein-cloverage 1.2.0.

## [0.3.1] - 2020-02-19

### Changed

- Upgrade ring/ring-core 1.8.0.
- Upgrade cloverage 1.1.2.
- Upgrade lein-codox 0.10.7.
- Upgrade org.clojure/clojure 1.10.1.

## [0.3.0] - 2019-04-24

### Changed

- Upgrade org.clojure/core.incubator 0.1.4.
- Upgrade org.clojure/data.codec 0.1.1.
- Upgrade lein-cloverage 1.1.1.
- Upgrade ring/ring-core 1.7.1.
- Upgrade compojure 1.6.1.
- Upgrade org.clojure/clojure 1.10.0.

### Removed

- Remove support for clojure 1.5.1, 1.6.0, and 1.7.0.

## [0.2.0] - 2019-04-24

### Changed

- `:okta-home` is a required argument to `wrap-okta`.
- clojure and ring-core are set to `provided` scope.
- Upgrade clojure 1.8.0.
- Change groupId to bostonaholic.

## [0.1.6] - 2015-12-02

### Changed

- Upgrade saml-toolkit 1.0.12.
- Upgrade ring 1.4.0.
- Upgrade compojure 1.4.0.

## [0.1.5] - 2015-07-17

### Fixed

- Fix Okta user that cannot be detected when accessing an unprotected route.

## [0.1.4] - 2015-06-16

### Changed

- Missing `:okta-home` option now throws `java.lang.AssertionError`.
- Minimum Clojure version set to 1.5.1.

## [0.1.3] - 2014-09-26

### Fixed

- Fix a bug where logout would lose information in the request object to be passed to the redirect.

## [0.1.2] - 2014-09-19

### Fixed

- Fix a bug where a route would not be skipped if it's matching route method came after a pair with a matching route path. e.g. `:skip-routes [:get "/about" :post "/about"]` the `:post "/about"` would not have been skipped.

## [0.1.1] - 2014-09-10

### Changed

- Package is now deployed as ring-okta instead of ring/ring-okta.

## [0.1.0] - 2014-09-09

### Added

- Initial release.

[Unreleased]: https://github.com/bostonaholic/ring-okta/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/bostonaholic/ring-okta/compare/v1.0.7...v1.1.0
[1.0.7]: https://github.com/bostonaholic/ring-okta/compare/v1.0.6...v1.0.7
[1.0.6]: https://github.com/bostonaholic/ring-okta/compare/v1.0.5...v1.0.6
[1.0.5]: https://github.com/bostonaholic/ring-okta/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/bostonaholic/ring-okta/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/bostonaholic/ring-okta/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/bostonaholic/ring-okta/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/bostonaholic/ring-okta/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/bostonaholic/ring-okta/compare/v0.5.1...v1.0.0
[0.5.1]: https://github.com/bostonaholic/ring-okta/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/bostonaholic/ring-okta/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/bostonaholic/ring-okta/compare/v0.3.2...v0.4.0
[0.3.2]: https://github.com/bostonaholic/ring-okta/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/bostonaholic/ring-okta/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/bostonaholic/ring-okta/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/bostonaholic/ring-okta/compare/v0.1.6...v0.2.0
[0.1.6]: https://github.com/bostonaholic/ring-okta/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/bostonaholic/ring-okta/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/bostonaholic/ring-okta/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/bostonaholic/ring-okta/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/bostonaholic/ring-okta/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/bostonaholic/ring-okta/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/bostonaholic/ring-okta/releases/tag/v0.1.0
