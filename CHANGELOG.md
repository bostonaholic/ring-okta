# Changelog

All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/) and [semver.org](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Changed
- Upgrade clojure 1.11.1
- Upgrade ring-core 1.11.0.

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

[Unreleased]: https://github.com/bostonaholic/ring-okta/compare/v1.0.6...HEAD
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
