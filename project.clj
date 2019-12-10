(defproject    day8/lein-git-inject "lein-git-inject/version"
  :description "Injects project.clj with some execution context."
  :url         "https://github.com/day8/lein-git-inject"
  :license     {:name "EPL-2.0"
                :url "https://www.eclipse.org/legal/epl-2.0/"}

  :eval-in-leiningen true

  :dependencies [[me.arrdem/cuddlefish "0.1.0"]]

  :plugins      [[day8/lein-git-inject "0.0.4"]]

  :middleware   [leiningen.git-inject/middleware]

  :release-tasks [["vcs" "assert-committed"]
                  ["deploy" "clojars"]]

  :deploy-repositories [["clojars" {:sign-releases false
                                    :url           "https://clojars.org/repo"
                                    :username      :env/CLOJARS_USERNAME
                                    :password      :env/CLOJARS_PASSWORD}]])
