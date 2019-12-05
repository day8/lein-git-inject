[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

This Leiningen middleware facilitates embedding certain values in your ClojureScript application which were ambient at the time it was built.

So, your application can contain one or more `def`s and they can be made to hold values known (only) at build time, like: 
   - the `git tag` for the source code being used to build the app (the equivalent to what would be returned by `git describe --tags --dirty --long`)
   - the build date/time
   - the user doing the build

So, you can "inject" these values into your built application/library, *_and then use those values for purposes like logging_*. 

## How It works

The magic happens in two steps and this middleware handles the first of them. 

It processes the `edn` within your `defproject` which is, of course, itself
within your `project.clj` file.  This middleware effectively does
a search and replace on this `edn`.  It searches for a small set of specific keywords or strings
(four of them) and, when it finds one of them, it replaces it with a value from the build context.

The second step is to use `:clojure-defines` to push values within the `defproject` itself into 
`def`s within your applciation. 

## How To Use It

Here's how to create and coordinate those two steps in your `project.clj` ...

```clojure

;; This note applies to the first line below.  
;; Normally, a "substitution key" like :lein-git-inject/version can be used in the edn 
;; as either a string or a keyword, but with the `defproject` version 
;; you must use the string variant, if you are using Cursive. It is a long story. 

(defproject day8/lein-git-inject-example "lein-git-inject/version"

  ...

  :plugins      [[day8/lein-git-inject "0.0.2"]   ;; <--- you must include this plugin
                 [lein-shadow          "0.1.7"]]

  :middleware   [leiningen.git-inject/middleware]  ;; <-- you must include this middleware
  
  
  ;; Assuming you are using the shadow-clj compiler, below is an example of how to 
  ;; combine this middleware with a `:clojure-define` in order to 
  ;; inject an ambient build value into a def within your application.
  ;; 
  ;; First, notice the use of the substitution key ":lein-git-inject/version".  
  ;; At build time, that will be replaced with the value for git tag. 
  ;; In turn, that value is used within a `:clojure-define`
  ;; to place it into a def (called "version" within the namespace "some.namespace"). 
  :shadow-cljs {:builds {:app {:target :browser
                               :release {:compiler-options {:closure-defines {some.namespace.version  :lein-git-inject/version}}}}}}

  ;; Note: by default, lien will change version in project.clj when doing a `lien release`. 
  ;; To avoid this (because the version will now come from the git tag), explicitly include
  ;; these steps to avoid using the default release process provided by lein. 
  :release-tasks [["vcs" "assert-committed"]
                  ["deploy"]])
```

## Substitutaions 

This middleware performs search and replace on four `substitution keys` within `defproject` edn. 

|   substituion key                    |    example replacement |
|--------------------------------------|-----------------------------|
| :lein-git-inject/version             |  "0.0.1"
| :lein-git-inject/build-iso-date-time |  "2019-11-18T00:05:02.273361"  |      
| :lein-git-inject/build-iso-date-week |  "2019-W47-2"
| :lein-git-inject/user-name           | "Isaac"    |
 
Replacement will happen for both the keyword version of these keys and the string form. 

## License

Copyright © 2019 Mike Thompson

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.
