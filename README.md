[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

This Leiningen middleware allows you to embed "build context values" into your ClojureScript application. 

Your application can contain one or more `def`s which hold these values, like: 
   - the `git tag` for the build  (kind of the equivalent of what would be returned by `git describe --tags --dirty --long`)
   - the build date/time
   - the user who did the build

So, you can "inject" values into your built application/library, and those values can then be used for purposes like logging. 

## How It works

This Leiningen middleware processes the `edn` within your `defproject` which is, of course, 
within your `project.clj` file.  This middleware effectively does
a search and replace on this `edn`.  It searches for a small set of specific strings or 
keyworks (four of them) and, when it finds one of them, it replaces it with a value from the build context.

In that way, you can inject build information into your `defproject` and that, in turn, means this
information can be embedded/injected into the built application as `defs` via the use of 
`:clojure-defines`. 

So the magic happens in two steps and this middleware handles the first step. 

The primary use case here is to embed/inject the build's associate git tag as the application version. 

## How To Use It

Here's how to use it in your `project.clj` ...

```clojure

;; This note applies to the first line below.  Normally a "keys" like 
;; :lein-git-inject/version can be used in the edn has either a string
;; or a keyword, but in the case of the defproject version 
;; you must use the string varient if you are using Cursive. A special case. 

(defproject day8/lein-git-inject-example "lein-git-inject/version"

  ...

  :plugins      [[day8/lein-git-inject "0.0.2"]   ;; <--- you must include this plugin
                 [lein-shadow          "0.1.7"]]

  :middleware   [leiningen.git-inject/middleware]  ;; <-- you must include this middleware
  
  
  ;; Assuming you are using the shadow-clj compiler, below is an example of how to 
  ;; combine this middleware with a :clojure-define in order to 
  ;; inject an aspect of the build context into a def within your application.
  ;; 
  ;; First, notice the use of ":lein-git-inject/version".  
  ;; At build time, that will be replaced with the value for git tag. 
  ;; In turn, that value will be used within a :clojure-define
  ;; to place it into a def (called "version") within the namespace "some.namespace". 
  :shadow-cljs {:builds {:app {:target :browser
                               :release {:compiler-options {:closure-defines {some.namespace.version  :lein-git-inject/version}}}}}}

  ;; Note: by default lien will change versions in project.clj when releasing 
  ;; To avoid this (because version will now come from git tag), explicitly include
  ;; these steps to avoid using the default process provided by lein. 
  :release-tasks [["vcs" "assert-committed"]
                  ["deploy"]])
```

## Substitutaions 

This middleware supports replacement of four keys:

|         key                          |    example replacement |
|--------------------------------------|-----------------------------|
| :lein-git-inject/version             |  "0.0.1"
| :lein-git-inject/build-iso-date-time |  "2019-11-18T00:05:02.273361"  |      
| :lein-git-inject/build-iso-date-week |  "2019-W47-2"
| :lein-git-inject/user-name           | "Isaac"    |


## License

Copyright © 2019 Day8 Technology Pty Ltd 

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.
