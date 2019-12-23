[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

Normally, when using Leiningen, the `version` of your application or library is encoded
as the 2nd argument to `defproject` (within your `project.clj` file): 
```
(defproject day8/my-app-name "23.4.5"    ;;  <--- "23.4.5" is the version
   ...)
```

This Leiningen middleware changes how a `version` is encoded.  It allows you to:
   1. construct the `version` from ***the ambient git context*** at ***build time*** and, specifically, from the latest git tag
   2. alter the lein `defproject` to use this constructed `version` in the build process
   3. optionally, embed this `version` value within your ClojureScript application, for purposes like logging 
   4. optionally, embed certain other ambient build-time values (like a timestamp) into your ClojureScript application

Regarding points 3 and 4, your application will contain one or more `def`s and they will be bound to these build-time values.

## A Git Backgrounder 

You are at the command line in a git repo, and you execute:
```sh
$ git describe --tags --dirty=-dirty
```
which will produce output like this:
```sh
v1.0.4-3-g975b-dirty
```
which encodes four (hyphen separated) pieces of data which we refer to as "the ambient git context":
  - the latest tag: "v1.0.4"
  - the number of commits you are "ahead" of that latest tag: "3" 
  - the short ref (SHA) for the commit referenced by that latest tag: "g975b"
  - an indication that there are uncommitted changes: "dirty"  (or absent)
  
This middleware will construct a `version` from these values, at build time, using two rules:
  - when the "ahead" count is 0, and the repo is not dirty, the `version` will be the tag (eg: `1.0.4`)
  - when the "ahead" count is non-zero, or the repo is dirty, the `version` will be the tag suffixed with `-<ahead-count>-<short-ref>-SNAPSHOT`; e.g. `1.0.4-3-g975b-SNAPSHOT`

## Latest Tag?

Above, we said a `version` was constructed using the "latest tag" but that was a white lie, to initially facilitate understanding.

The full truth is:
  1. what's used is the latest "version tag" found in the commit history
  2. by default, a "version tag" is any tag which matches the regex: `#"^version\/(\d+\.\d+\.\d+)$"`
  3. so, a version tag would look like this: `version/1.2.3`  (that's the string `version/` followed by a semver)
  4. You can, optionally, override this regex with your own (see below) to specify a different structure for your version tags
  5. This middleware will inspect the current branch's history for tags and it will find the latest one which matches the regex, and it is THAT tag which used to construct a version, and it is that tag against which the "ahead count" will be calculated, etc.
  
Sharp edges to be aware of:
  - if no matching tag is found then the "version" constructed will be `version-unavailable`
  - this middleware will obtain the "ambient git context" by shelling out `git` commandline. If this commandline is not available, then you'll see error messages and the `version` constructed will be `version-unavailable`
  - this approach does have one potential dark/confusing side which you'll probably want to guard against: misspelling you tag. Let's say you tag with `ersion/1.2.3` (oops typo) which means the regex won't match, the tag will be ignored, and an earlier tag will be used. Which is not what you intended. Which is bad. To guard against this, you'll want to add a trigger in your repo to verify/assert that any tags added conform to a small set of allowable cases like `version/*` or `doc/.*`. See github actions. 
  - `lein release` will massage the `version` in your `project.clj` in unwanted ways unless you take some action (see the example below regarding that action)

## Three Steps

The entire process has three steps, and this middleware handles the first two of them. 

As you read these three steps, please keep in mind that Leiningen middleware runs 
very early in the Lein build pipeline. So early, in fact, that it can alter the `EDN` 
of your `defproject` (within your `project.clj` file).

***First***, it will construct a `version` value from the ambient git context using the two "construction rules" above.

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
to see the the entire project map after the substitutions have taken place.

***Note #2:*** Design decision: we deliberately choose keys to be strings (not keywords), 
because, when you are using Cursive,
you can't have 2nd argument to `defproject` (the version!) be a keyword.
Only a string can go there
because Cursive does some inspection of your `project.clj` ahead of any lein use. So, we
decided to not support keyword keys, only strings, even though keywords seems like the idiomatic choice.
And it is better if there is only be one way to do something. 


## Example

Here's how your `project.clj` should be arranged to achieve the three steps described above ...

```clojure

;; On the next line, note that the version (2nd argument of defproject) is a 
;; substitution key which will be replaced by the "constructed version" which is
;; built from the git context, using two rules.
(defproject day8/lein-git-inject-example "lein-git-inject/version"
  ...

  :plugins      [[day8/lein-git-inject "0.0.5"]   ;; <--- you must include this plugin
                 [lein-shadow          "0.1.7"]]

  :middleware   [leiningen.git-inject/middleware]  ;; <-- you must include this middleware
  
  
  ;; If you are using the shadow-clj compiler and lein-shadow the
  ;; shadow-cljs configuration is in project.clj. Below is an example of how to 
  ;; combine this middleware with a `:clojure-define` in order to 
  ;; inject values into your application.
  ;; 
  ;; You'll notice the use of the substitution key "lein-git-inject/version".  
  ;; At build time, this middleware will replace that keyword with the constructed
  ;; version. In turn, that value is used within a `:clojure-define` to place
  ;; it into a `def` (called `version` within the namespace `some.namespace`). 
  :shadow-cljs {:builds {:app {:target :browser
                               :release {:compiler-options {:closure-defines {some.namespace.version  "lein-git-inject/version"}}}}}}

  ;; Note: by default, lein will change the version in project.clj when you do a `lein release`. 
  ;; To avoid this (because you now want the version to come from the git context at build time), 
  ;; explicitly include the following steps to avoid using the default release process provided by lein. 
  :release-tasks [["vcs" "assert-committed"]
                  ["deploy"]]

  ;; Optional configuration 
  ;; If you want to use your own regex to identify version tags you can supply it. 
  ;; When designing your tag structure, remember that that git tags are git references 
  ;; and follow the rules about well formedness. Eg: no ":". See https://git-scm.com/docs/git-check-ref-format
  ;; Note: the regex you supply should: 
  ;;  - "match" a version tag and 
  ;;  - it should return one capturing group which extracts the actual version to use. In the example below, 
  ;;    the regex will match the tag "v/1.2.3" but it will capture the "1.2.3" part as the version. 
  :git-inject {
    :version-pattern  #"^v\/(.*)" }
)
```


## License

Copyright © 2019 Mike Thompson

Derived from cuddlefish © 2018 Reid "arrdem" McKenzie

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

Distributed under the Eclipse Public License, the same as Clojure.


