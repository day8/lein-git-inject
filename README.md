[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

Normally, when using lein, the `version` of an application or library is found by 
looking in the `project.clj` and locating the 2nd argument of the `defproject`: 
```
(defproject day8/my-app-name "23.4.5"    ;;  <--- "23.4.5" is the version
   ...)
```

This Leiningen middleware allows you to:
   1. create a `version` at build time, and have this version be derived from the ambient git context, specifically the tags
   2. alter the lein `defproject` to use this derived `version` in the build process
   3. optionally, embed this `version` value within your ClojureScript application for purposes like logging 
   4. optionally, embed certain other ambient build-time values (like a timestamp) into your ClojureScript application

Regarding points 3 and 4, your application will contain one or more `def`s and they will be bound to the build-time values. You can then ***use these values for purposes like logging***.

## A Git Backgrounder 

Assume for a minute that you are at the commandline in a git repo, and you type:
```sh
$ git describe --tags --XXX --dirty=dirty
```
you will see a string response like:
```sh
v1.0.4-14-g975b-dirty
```

That string encodes four pieces of information:
  - the most recent tag "v1.0.4"
  - how many commits you are "ahead" of the tag: "14" 
  - a SHA for XXX
  - an indication that there are uncommitted changes: dirty
  
It is from these four values that this utility will build a `version` for your application or libary. 

The most important of these four values is the tag. 

XXX more here

## How It works

The entire process has three steps, and this middleware handles the first two of them. 

As you read these three steps, please keep in minid that Leiningen middleware runs 
very early in the Lein build pipeline. So early, in fact, that it can alter the `EDN` 
of your `defproject` (within your `project.clj` file).

***First***, it will derive a `version` value from the ambient git context

***Second***, it does a particular search and replace on the `EDN` in 
the `defproject`.  It searches for
four special keywords or strings - referred to as `substitution keys` - 
and, when it finds one of them, it replaces that key with the associated 
value from the build context.  In particular, it replaces any occurance of the 
substitution key `:lein-git-inject/version` with the `version` derived in step 1.

***Third***, if you are compiling using a tool like shadow-clj, you can use the 
`:clojure-defines` feature to push/embed values within the 
`defproject` itself into `def`s within your application.


## The Four Substitution Keys 

This middleware performs search and replace on four `substitution keys` 
within the `EDN` of your `defproject`. 
It will search for these values as keywords or strings.  


|   substituion key                    |    example replacement      |
|--------------------------------------|-----------------------------|
| :lein-git-inject/version             |  "0.0.1"                    |
| :lein-git-inject/build-iso-date-time |  "2019-11-18T00:05:02.273361"  |      
| :lein-git-inject/build-iso-date-week |  "2019-W47-2"               |
| :lein-git-inject/user-name           | "Isaac"                     |

***Note:*** To debug these substitutions, you can use `lein pprint` 
to see the the entire project map after the substitutions have taken place.
 
## How To Use It

Here's how your `project.clj` should be arranged to achive the three steps described above ...

```clojure

;; This note applies to the first line below.  
;; Normally, a "substitution key" like :lein-git-inject/version can be 
;; used within the EDN in either its string-form or as a keyword, either way is fine. 
;; But within the `defproject` "version" you must use the string variant, 
;; IF YOU ARE USING CURSIVE, because Cursive does some inspection
;; of your project.clj ahead of any lein use - and it doesn't like to 
;; see a keyword where a string is expected, in this one case.

(defproject day8/lein-git-inject-example "lein-git-inject/version"

  ...

  :plugins      [[day8/lein-git-inject "0.0.2"]   ;; <--- you must include this plugin
                 [lein-shadow          "0.1.7"]]

  :middleware   [leiningen.git-inject/middleware]  ;; <-- you must include this middleware
  
  
  ;; Assuming you are using the shadow-clj compiler and lein-shadow so that the
  ;; shadow-cljs configuration is in project.clj, below is an example of how to 
  ;; combine this middleware with a `:clojure-define` in order to 
  ;; inject an ambient build value into a def within your application.
  ;; 
  ;; You'll notice the use of the substitution key ":lein-git-inject/version".  
  ;; At build time, this middleware will replace that keyword with a git-derived 
  ;; value. In turn, that value is used within a `:clojure-define` to place
  ;; it into a `def` (called `version` within the namespace `some.namespace`). 
  :shadow-cljs {:builds {:app {:target :browser
                               :release {:compiler-options {:closure-defines {some.namespace.version  :lein-git-inject/version}}}}}}

  ;; Note: by default, lein will change the version in project.clj when you do a `lein release`. 
  ;; To avoid this (because you now want the version to come from the git context at build time), 
  ;; explicitly include the following steps to avoid using the default release process provided by lein. 
  :release-tasks [["vcs" "assert-committed"]
                  ["deploy"]]

  ;; Optional configuration which specifies how to "derive" the version from git content.
  :git-inject {
    ;; choose to ignore ahead or dirty state like this:
    :ignore-ahead?            true
    :ignore-dirty?            true

    ;; Optional: you may customize the patterns used to extract versions from
    ;; tags like below. Note only ahead (or ignore-ahead?) and dirty
    ;; (or ignore-dirty?) state is used to choose between release or snapshot
    ;; versions. The below patterns simply extract values from the tag to be
    ;; injected.
    :release-version-pattern  #"v?(.*)"
    :snapshot-version-pattern #"v?(\d+)\.(\d+)\.(\d+)(-.+)?"

    :git-describe->version [ {#"v([0-9]*.[0-9]*.[0-9])-[1-9][0-9]+.*"   #""}
                  ]
    })
```


## License

Copyright © 2019 Mike Thompson

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.
