(ns drift.test-generator
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [config.finished-config]
            [drift.builder :as builder]
            [drift.config]
            [drift.generator :refer [create-file-content generate-file-content generate-migration-file-cmdline migration-usage]])
  (:import (java.nio.file Files Path)
           (java.nio.file.attribute FileAttribute)))

(deftest test-migration-usage
  (is (not (string/blank? (with-out-str (migration-usage))))))

(deftest test-create-file-content
  (is (re-matches #"(?s)\(ns migrations.001-create-tests.*\(defn up.*\(defn down.*"
                  (create-file-content "migrations.001-create-tests" nil nil nil))))

(deftest test-generate-file-content
  (try
    (let [^Path temp-file (Files/createTempFile (builder/find-or-create-migrate-directory)
                                                "migration" ".clj" (make-array FileAttribute 0))]
      (try
        (generate-file-content temp-file nil nil nil)
        (is (re-matches #"(?s)\(ns migrations.migration.*\(defn up.*\(defn down.*"
              (slurp (Files/newBufferedReader temp-file))))
        (finally (Files/delete temp-file))))
    (catch Exception e (throw e))))

(deftest test-generate-migration-file-cmdline
  (with-redefs [drift.generator/generate-migration-file
                (fn [mn]
                  (is (= drift.config/*config-fn-symbol* 'foo.bar/baz))
                  (is (= mn "blahblah")))]

    (generate-migration-file-cmdline
      nil
      ["-c" "foo.bar/baz" "blahblah"])))

(deftest test-finished-fn-called
  (with-redefs [builder/find-or-create-migrate-directory (fn [])
                builder/create-migration-file (fn [_ _])
                drift.generator/generate-file-content (fn [_ _ _ _])]

    (generate-migration-file-cmdline nil ["-c" "config.finished-config/migrate-config" "blahblah"])

    (is (= @config.finished-config/finished-run? true))))
