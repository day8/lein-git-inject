(ns leiningen.git-inject-test
  (:require
    [clojure.test :refer [deftest is]]
    [leiningen.git-inject :refer [middleware]]))

(deftest middleware-test
  (let [project  {:version     :lein-git-inject/version
                  :shadow-cljs {:builds {:app {:target           :browser
                                               :compiler-options {:closure-defines {'app/version :lein-git-inject/version}}}}}
                  :timestamp   :lein-git-inject/build-iso-date-time
                  :username    :lein-git-inject/username}
        project' (middleware project)]
    (is (string? (:version project')))
    (is (map? (:shadow-cljs project')))
    (is (string? (get-in project' [:shadow-cljs :builds :app :compiler-options :closure-defines 'app/version])))
    (is (string? (:timestamp project')))
    (is (string? (:username project')))))
