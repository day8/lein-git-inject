# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.0.15] - 2021-06-15

#### Added

- Add `lein-git-inject/sha` with configurable `:sha-length`. See [#4](https://github.com/day8/lein-git-inject/pull/4).
  Thanks to [@Quezion](https://github.com/Quezion).

## [0.0.14] - 2020-04-21

#### Fixed

- Fix compatibility with leiningen versions older than 2.9.0. 0.0.13 introduced
  a regression in older leiningen versions by using clojure.core/boolean? which
  is only available from Clojure 1.9 and leiningen 2.9.0 onwards.

## [0.0.13] - 2020-04-01

#### Fixed

- Fix ignore-dirty? flag boolean values. See [#3](https://github.com/day8/lein-git-inject/issues/3).

## [0.0.12] - 2020-03-24

#### Fixed

- Fix default version pattern by anchoring to start and end of string

## [0.0.11] - 2020-01-15

#### Added

- Add support for environment variables as values of ignore-dirty? config

## [0.0.10] - 2020-01-13

#### Fixed

- Fix support for multiple tags on a single commit. 

## [0.0.9] - 2020-01-09

#### Added

- Add ignore-dirty? config

## [0.0.8] - 2020-01-09

#### Fixed

- Fix support for unusual git histories; i.e. more than result for
  `git rev-list --max-parents=0 HEAD`

## [0.0.7] - 2020-01-07

### Breaking 

Changed the default tag structure from `version/N.N.N` to `vN.N.N`
