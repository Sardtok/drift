(ns drift.test-version
  (:use [clojure.test]
        [drift.version]))

(deftest test-current-db-version
  (is (= (current-db-version) 0)))

(deftest test-update-db-version
  (is (= (current-db-version) 0))
  (update-db-version 1)
  (is (= (current-db-version) 1))
  (update-db-version 0)
  (is (= (current-db-version) 0)))
