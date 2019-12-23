[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

Normally, Leiningen projects list an explicit `version` as the 2nd argument to the `defproject` 
(within your `project.clj` file), like this: 
```
(defproject my-app "3.4.5"    ;;  <--- "3.4.5" is the version
   ...)
```

This Leiningen middleware changes how the `version` is obtained: 
   1. it constructs a `version` from ***the ambient git context*** at ***build time***
   2. it alters the lein `defproject` to use this constructed `version` in the build process
   
As an added bonus, it also, optionally, facilitates the embedding of this `version` (along with other built-time values) 
within your applications for purposes like run-time logging. 

## A Git Backgrounder 

Imagine you are at the command line in a git repo, and you execute:
```sh
$ git describe --tags --dirty=-dirty
```
which will produce output similar to:
```sh
v1.0.4-3-g975b-dirty
```
which encodes four (hyphen separated) pieces of data which we refer to as "the ambient git context":
  - the latest git tag: "v1.0.4"
  - the number of commits the repo is currently "ahead" of that latest tag: "3" 
  - the short ref (SHA) for the commit referenced by that latest tag: "g975b"
  - an indication that there are uncommitted changes: "dirty"  (or absent)
  
This middleware will construct a `version` from these values, at build-time, using two rules:
  - when the "ahead" count is 0, and the repo is not dirty, the `version` will be the tag (eg: `1.0.4`)
  - when the "ahead" count is non-zero, or the repo is dirty, the `version` will be the tag suffixed with `-<ahead-count>-<short-ref>-SNAPSHOT`, e.g. `1.0.4-3-g975b-SNAPSHOT`

## Latest Tag?

So far we have said that a `version` is constructed using the "latest tag". While that's often true, it is not the full story. 

The full truth is: 
  1. what's used is the latest "version tag" found in the commit history
  2. a "version tag" is any tag which matches the regex: `#"^version\/(\d+\.\d+\.\d+)$"`. So, it is a tag with a specific textual structure.
  3. a version tag might look like this: `version/1.2.3`  (that's the string `version/` followed by a semver structure)
  4. you can override this default regex with your own (see how below)
  
So, this middleware will traverse backwards the current branch's history looking for a tag which mathes the regex, and when it finds one, it is THAT tag which is used to construct a version - it is that tag against which the "ahead count" will be calculated, etc.
  
Sharp edges to be aware of:
  - if no matching tag is found then the "version" constructed will be `version-unavailable`
  - this middleware will obtain the "ambient git context" by shelling out to the `git` executable. If this executable is not in the path, then you'll see messages on stderr and the `version` constructed will be `version-unavailable`
  - this design does have one potential dark/confusing side which you'll probably want to guard against: misspelling your tag. Let's say you tag with `ersion/1.2.3` (can you see the typo?) which means the regex won't match, the tag will be ignored, and an earlier tag (without a typo) will be used. Which is not what you intended. And that's bad. To guard against this, you'll want to add a trigger (Github action ?) to  your repo to verify/assert that any tags added conform to a small set of allowable cases like `version/*` or `doc/.*`.  That way any mispelling will be flagged because the tag would fail to match an acceptable, known syructure. Something like that.
  - `lein release` will massage the `version` in your `project.clj` in unwanted ways unless you take some action (see the "Example" below regarding how to avoid this unwanted behaviour) 

## The Three Steps

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

Here's how to write your `project.clj` to achieve the three steps described above ...

```clojure

;; On the next line, note that the version (2nd argument of defproject) is a 
;; substitution key which will be replaced by the "constructed version" which is
;; built from the ambient git context, using the two rules.
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


