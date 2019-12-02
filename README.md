[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

A Leiningen middleware to inject `project.clj` with some execution context.

The main use case is to inject a git tag-based version. The difference to other
previous lein git plugins is the ability to replace values at any location such
as in `:closure-defines` maps in heavily nested compiler configuration.

## Usage

Replaces any value, at any level of nesting, in `project.clj` with a supported
string OR keyword such as `:lein-git-inject/version`.

```clojure
(defproject    day8/lein-git-inject-example "lein-git-inject/version"
  :description "An example."
  :url         "https://github.com/day8/lein-git-inject"
  :license     {:name "EPL-2.0"
                :url "https://www.eclipse.org/legal/epl-2.0/"}

  :eval-in-leiningen true

  :dependencies [[me.arrdem/cuddlefish "0.1.0"]]

  :plugins      [[day8/lein-git-inject "0.0.2"]
                 [lein-shadow          "0.1.7"]]

  :middleware   [leiningen.git-inject/middleware]

  :shadow-cljs {:builds {:app {:target :browser
                               :release {:compiler-options {:closure-defines {;; "0.0.1"
                                                                              day8.example.version         :lein-git-inject/version
                                                                              ;; "2019-11-18T00:05:02.273361"
                                                                              day8.example.build-date-time :lein-git-inject/build-iso-date-time
                                                                              ;; "2019-W47-2"
                                                                              day8.example.build-week-date :lein-git-inject/build-iso-date-week
                                                                              ;; "isaac" i.e. the local shell username.
                                                                              day8.example.username        :lein-git-inject/user-name}}}}}}

  :release-tasks [["vcs" "assert-committed"]
                  ["deploy" "clojars"]]

  :deploy-repositories [["clojars" {:sign-releases false
                                    :url           "https://clojars.org/repo"
                                    :username      :env/CLOJARS_USERNAME
                                    :password      :env/CLOJARS_PASSWORD}]])

```

## License

Copyright © 2019 Day8 Technology Pty Ltd 

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.