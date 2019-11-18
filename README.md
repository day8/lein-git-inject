[![CI](https://github.com/day8/lein-git-inject/workflows/ci/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=ci)
[![CD](https://github.com/day8/lein-git-inject/workflows/cd/badge.svg)](https://github.com/day8/lein-git-inject/actions?workflow=cd)
[![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/tags)
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)
[![GitHub issues](https://img.shields.io/github/issues-raw/day8/lein-git-inject?style=flat)](https://github.com/day8/lein-git-inject/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/day8/lein-git-inject)](https://github.com/day8/lein-git-inject/pulls)
[![License](https://img.shields.io/github/license/day8/lein-git-inject.svg)](LICENSE)

# lein-git-inject

A Leiningen middleware to inject `project.clj` with some execution context.

Only a few useful known replacements are currently supported.

## Usage

### Step 1. Add Plugin

Add the following dependency into the `:plugins` vector of `project.clj`: <br>
[![Clojars Project](https://img.shields.io/clojars/v/day8/lein-git-inject.svg)](https://clojars.org/day8/lein-git-inject)

### Step 2. Add Middleware

Add the following to the `:middleware` vector of `project.clj`: <br>

```clojure
:middleware [leiningen.git-inject/middleware]
```

### Step 3. Replace Values

Replace any value, at any level of nesting, in `project.clj` with a supported
keyword such as `:lein-git-inject/version`.

#### Git-based version

```clojure
:lein-git-inject/version
;; ->
"0.0.1"
```

#### ISO-like datetime

```clojure
:lein-git-inject/build-iso-date-time
;; ->
"2019-11-18T00:05:02.273361"
 ```

#### ISO week-based date

```clojure
:lein-git-inject/build-iso-week-date
;; ->
"2019-W47-2"
```

## Username

```clojure
:lein-git-inject/user-name
;; ->
"isaac"
```

## License

Copyright © 2019 Day8 Technology Pty Ltd 

Derived from lein-git-version © 2017 Reid "arrdem" McKenzie

Derived from lein-git-version © 2016 Colin Steele

Derived from lein-git-version © 2011 Michał Marczyk

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
