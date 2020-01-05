[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

Leiningen projects normally provide an explicit `version` as the 2nd argument to `defproject` 
(within the `project.clj` file), like this: 
```clj
(defproject my-app "3.4.5"    ;;  <--- "3.4.5" is the version
   ...)
```

This Leiningen middleware instead derives `version` from ***the ambient git context*** and, in particular, the ***latest git tag***.

Your `defproject` will nominate a placeholder string, "lein-git-inject/version", where normally an explicit version would be expected, like this: 
```clj
(defproject my-app "lein-git-inject/version"
   ...)
```

Then, at build time, this middleware will:
   1. use ***a two-rule method*** to derive the "version" from ***the ambient git context***. We refer to this as `the derived version`.
   2. replace the placeholder string "lein-git-inject/version" with `the derived version`
   
As an added bonus, it also facilitates embedding `the derived version` (and certain other build-time values) 
within your ClojureScript application, making it readily available at run-time for purposes like logging.

## The Ambient Git Context

Imagine you are at the command line in a git repo, and you execute:
```sh
$ git describe --tags --dirty=-dirty
```
Assuming that the latest tag in your repo was `version/1.0.4`, this command might output something like:
```sh
version/1.0.4-3-g975b-dirty
```
which encodes four (hyphen separated) values which we refer to as "the ambient git context":
  - the latest git tag: "version/1.0.4"
  - the number of commits the repo is currently "ahead" of that latest tag: "3" 
  - the short ref (SHA) for the commit referenced by that latest tag: "g975b"
  - an indication that there are uncommitted changes: "dirty"  (or absent)
  
## The Two-Rule Method

This middleware creates `the derived version` from these four "ambient" values by applying two rules:
  1. when the "ahead" count is 0, and the repo is not dirty, `the derived version` will just be the latest tag (eg: `1.0.4`)
  2. when the "ahead" count is non-zero, or the repo is dirty, `the derived version` will be the tag suffixed with `-<ahead-count>-<short-ref>-SNAPSHOT`, e.g. `1.0.4-3-g975b-SNAPSHOT`
  
 ***Note:*** the attentive reader will notice that only part of the latest tag is used (`1.0.4` rather than `version/1.0.4`). This is explained within the next section. 

## The Latest Tag

So far, we have said that `the derived version` is created using the "latest tag". While that's often true, it is not the whole story, which is actually as follows:
  1. what's used is the "latest version tag" found in the commit history  (not the "latest tag")
  2. where a "version tag" is a tag with a specific textual structure
  3. that textual structure must match the regex: `#"^version\/(\d+\.\d+\.\d+)$"`
  4. so, one of these "version tags" might look like: `version/1.2.3`  (the string `version/` followed by a semver, `N.N.N`)
  5. tags which do not match the regex are ignored (which means you can use tags for other purposes, not just for nominating versions)
  6. you can override this default regex with one of your own which will recognise an alternative textual structure (see how below)
  7. you'll notice that the regex has a capturing group which extracts the semver part (N.N.N). If you provide your own regex, it must contain one capturing group which isolates that part of the tag to be used in `the derived version`.
  
So, this middleware will traverse backwards through the history of the current commit looking for a tag which has the right structure (matches the regex), and when it finds one, it is THAT tag which is used to create `the derived version` - it is that tag against which the "ahead count" will be calculated, etc.

## Sharp Edges
  
Some sharp edges you should be aware of:
  - if no matching tag is found then `the derived version` will be `git-version-tag-not-found`
  - this middleware obtains the "ambient git context" by shelling out to the `git` executable. If this executable is not in the PATH, then you'll see messages on `stderr` and `the derived version` will be `git-command-not-found`
  - this design has one potential dark/confusing side which you'll probably want to guard against: misspelling your tag. Let's say you tag with `ersion/1.2.3` (can you see the typo?) which means the regex won't match, the tag will be ignored, and an earlier version tag (one without a typo) will be used. Which is not what you intended. And that's bad. To guard against this, you'll want to add a trigger (GitHub Action ?) to  your repo to verify/assert that any tags added conform to a small set of allowable cases like `version/*` or `doc/.*`.  That way any misspelling will be flagged because the tag would fail to match an acceptable, known structure. Something like that
  - `lein release` will massage the `version` in your `defproject` in unwanted ways unless you take specific actions to stop it (see the "Example" below) 

## Three Steps

The entire process has three steps, and this middleware handles the first two of them. 

As you read these three steps, keep in mind that Leiningen middleware runs 
very early in the Lein build pipeline. So early, in fact, that it can alter the `EDN` 
of your `defproject` (within your `project.clj` file).

***First***, it will create `the derived version` from the "ambient git context", using "the two-rule method" detailed above.

***Second***, it will perform a search and replace on the `EDN` in 
the `defproject`.  It searches for
four special strings - referred to as `substitution keys` - 
and, when it finds one of them, it replaces that key with the associated 
value from the build context.  In particular, it replaces any occurrence of the 
substitution key `"lein-git-inject/version"` with the `version` constructed in step 1.

***Third***, if you are compiling using a tool like shadow-clj, you can use the 
`:clojure-defines` feature to push/embed values within the 
`defproject` itself into `def`s within your application, making those values 
available at run time for purposes like logging.


## The Four Substitution Keys 

This middleware performs search and replace on four `substitution keys` 
within the `EDN` of your `defproject`.
It will search for these strings:


|   substituion key                    |    example replacement      |
|--------------------------------------|-----------------------------|
| "lein-git-inject/version"             |  "12.4.1-2-453a730-SNAPSHOT"                    |
| "lein-git-inject/build-iso-date-time" |  "2019-11-18T00:05:02.273361"  |      
| "lein-git-inject/build-iso-date-week" |  "2019-W47-2"               |
| "lein-git-inject/user-name"           | "Isaac"                     |

***Note #1:*** To debug these substitutions, you can use `lein pprint` 
to see the entire project map after the substitutions have taken place.

***Note #2:***  Substitution keys are strings, even though 
keywords seem like the idiomatic choice. Why? Reason: when you are using Cursive,
the 2nd argument to `defproject` (the version!) can't be a keyword. 
Only a string can go there because Cursive does its own inspection of your `project.clj` 
indepentently of Lein and it doesn't work if there is a keyword there. 
So string keys were necessary. And there is less cognative load if there 
is only one way to do something - so we reluctantly said "no" to allowing keyword keys too.


## An Annotated Example

Here's how to write your `project.clj` to achieve the three steps described above...

```clojure

;; On the next line, note that the version (2nd argument of defproject) is a 
;; substitution key which will be replaced by `the derived version` which is
;; built from `the ambient git context`, using `the method`.
(defproject day8/lein-git-inject-example "lein-git-inject/version"
  ...

  :plugins      [[day8/lein-git-inject "0.0.5"]   ;; <--- you must include this plugin
                 [lein-shadow          "0.1.7"]]

  :middleware   [leiningen.git-inject/middleware]  ;; <-- you must include this middleware
  
  
  ;; If you are using the shadow-clj compiler and lein-shadow, the shadow-cljs 
  ;; configuration is put here in project.clj. Below is an example of how to 
  ;; combine this middleware with a `:clojure-define` in order to 
  ;; inject build-time values into your application, for later run-time use.
  ;; 
  ;; You'll notice the use of the substitution key "lein-git-inject/version".  
  ;; At build time, this middleware will replace that keyword with `the derived version`.
  ;; In turn, that value is used within a `:clojure-define` to bind it
  ;; to a var, via a `def` in your code (called `version` within the namespace `some.namespace`). 
  :shadow-cljs {:builds {:app {:target :browser
                               :release {:compiler-options {:closure-defines {some.namespace.version  "lein-git-inject/version"}}}}}}

  ;; Note: by default, lein will change the version in project.clj when you do a `lein release`. 
  ;; To avoid this (because you now want the version to come from the git context at build time), 
  ;; explicitly include the following steps to avoid using the default release process provided by lein. 
  :release-tasks [["vcs" "assert-committed"]
                  ["deploy"]]

  ;; Optional configuration 
  ;; Here is where you can supply an alternative regex to identify `version tags`. 
  ;; When designing your own textual structure for "version tags", remember that 
  ;; git tags are git references and that there are rules about well formedness. 
  ;; For example, you can't have a ":" in a tag. See https://git-scm.com/docs/git-check-ref-format
  ;; The regex you supply has two jobs:
  ;;  1. to "match" a version tag 
  ;;  2. to return one capturing group which extracts the text within the tag which is to 
  ;;     be used as the version. In the example below, the regex will match the tag "v/1.2.3" 
  ;;     but it will capture the "1.2.3" part and it is THAT part which will be used as the version. 
  :git-inject {
    :version-pattern  #"^v\/(.*)$" }
)
```


## License

Copyright © 2019 Mike Thompson

Derived from cuddlefish © 2018 Reid "arrdem" McKenzie

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.


