(ns leiningen.git-inject
  (:require
    [clojure.walk :as walk]
    [cuddlefish.core :as git]
    [clojure.string :as string])
  (:import
    (java.io IOException)
    (java.time LocalDateTime)
    (java.time.format DateTimeFormatter)))

(def default-config
  "The default configuration values."
  {:git                      "git"
   :describe-pattern         git/git-describe-pattern
   :ignore-ahead?            false
   :ignore-dirty?            false
   :release-version-pattern  #"v?(.*)"
   :snapshot-version-pattern #"v?(\d+)\.(\d+)\.(\d+)(-.+)?"})

(defn git-status-to-version
  [{:keys [ignore-ahead? ignore-dirty? release-version-pattern snapshot-version-pattern] :as config}]
  (try
    (let [{:keys [tag ahead ahead? dirty?]} (git/status (select-keys config [:git :describe-pattern]))]
      (if-not (string? tag)
        ;; If git status is nil (e.g. IntelliJ evaluating project.clj):
        "git-tag-unavailable"
        (if (and (or ignore-ahead? (not ahead?))
                 (or ignore-dirty? (not dirty?)))
          ;; If this is a release version:
          (let [[_ release-version] (re-find release-version-pattern tag)]
            (if (nil? release-version)
              ;; If tag is poorly formatted:
              "git-tag-invalid"
              ;; Otherwise we have a good release version:
              release-version))
          ;; Otherwise this is a snapshot version:
          (let [[_ major minor patch suffix] (re-find snapshot-version-pattern tag)]
            (if (nil? major)
              ;; If tag is poorly formatted:
              "git-tag-invalid"
              (let [patch' (try (Long/parseLong patch) (catch Throwable _ 0))
                    patch+ (inc patch')]
                ;; Otherwise we have a good snapshot version:
                (str major "." minor "." patch+ suffix
                     (when-not ignore-ahead? (str "-" ahead))
                     "-SNAPSHOT")))))))
    (catch IOException _
      ;; If git binary is not available (e.g. not in path):
      "git-unavailable")))

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
                     (reduce-kv
                       (fn [ret k f]
                         (cond
                           (keyword? x)
                           (if (= x k)
                             (f config)
                             ret)

                           (string? x)
                           (let [s (str (namespace k) "/" (name k))]
                             (if (string/includes? x s)
                               (string/replace x (re-pattern s) (f config))
                               ret))

                           :default
                           ret))
                       x
                       x->f))
                   project)]
    project'))
