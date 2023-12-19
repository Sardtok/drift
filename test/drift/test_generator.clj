(ns drift.test-generator
  (:require [clojure.test :refer [deftest is]]
            [config.finished-config]
            [drift.builder :as builder]
            [drift.config]
            [drift.generator :refer [create-file-content generate-migration-file-cmdline migration-usage]]))

(deftest test-migration-usage
  (migration-usage))

(deftest test-create-file-content
  (is (create-file-content "migrations.001-create-tests" nil nil nil)))

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
