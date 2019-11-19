(ns leiningen.git-inject
  (:require
    [clojure.walk :as walk]
    [cuddlefish.core :as git])
  (:import
    (java.time LocalDateTime)
    (java.time.format DateTimeFormatter)))

(def default-config
  "The default configuration values."
  {:git              "git"
   :describe-pattern git/git-describe-pattern})

(defn git-status-to-version
  [config]
  (let [{:keys [tag ahead ahead? dirty?]} (git/status config)]
    (if-not (string? tag)
      ;; If git status is nil (e.g. IntelliJ reading project.clj) then return...
      "git-tag-unavailable"
      (if (and (not ahead?) (not dirty?))
        (let [[_ version] (re-find #"v?(.*)" tag)]
          version)
        (let [[_ major minor patch suffix] (re-find #"v?(\d+)\.(\d+)\.(\d+)(-.+)?" tag)]
          (if (nil? major)
            ;; If tag is poorly formatted then return...
            "git-tag-invalid"
            (let [patch' (try (Long/parseLong patch) (catch Throwable _ 0))
                  patch+ (inc patch')]
              (str major "." minor "." patch+ suffix "-" ahead "-SNAPSHOT"))))))))

(def x->f
  {:lein-git-inject/build-iso-date-time (fn [_] (.format (LocalDateTime/now) DateTimeFormatter/ISO_DATE_TIME))
   :lein-git-inject/build-iso-week-date (fn [_] (.format (LocalDateTime/now) DateTimeFormatter/ISO_WEEK_DATE))
   :lein-git-inject/version             git-status-to-version
   :lein-git-inject/username            (fn [_] (System/getProperty "user.name"))})

(defn middleware
  [{:keys [git-inject]
    :as   project}]
  (let [config   (merge default-config git-inject)
        project' (walk/prewalk
                   (fn [x]
                     (let [k (if (string? x) (keyword x) x)]
                       (if-let [f (get x->f k)]
                         (f config)
                         x)))
                   project)]
    project'))
