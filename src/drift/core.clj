(ns drift.core
  (:require [clojure.string :as string]
            [drift.config :as config]
            [drift.utils :as utils])
  (:import (java.nio.file Files LinkOption Path Paths)
           (java.util Comparator TreeSet)))

(defn
  #^{:doc "Runs the init function with the given args."}
  run-init [args]
  (when-let [init-fn (config/find-init-fn)]
    (init-fn args)))

(defn run-finished
  "runs the finished function"
  []
  (when-let [finished-fn (config/find-finished-fn)]
    (finished-fn)))

(defn with-init-config
  "run the init fn, merge results into config, then call the next function with that config
   bound to drift.config/*config-map*"
  [args f]
  (let [init-fn (config/find-init-fn)
        init-config (if init-fn (init-fn args))]
    (config/with-config-map
      (merge (config/find-config) (if (map? init-config) init-config))
      (fn []
        (let [result (f)]
          (run-finished)
          result)))))

(defn
  #^{:doc "Returns the directory where Drift is running from."}
  user-directory []
  (Paths/get (.getProperty (System/getProperties) "user.dir") (make-array String 0)))

(defn find-migrate-dir-name []
  (let [migrate-dir-name (config/find-migrate-dir-name)]
    (if (.startsWith migrate-dir-name "/")
      (subs migrate-dir-name 1)
      migrate-dir-name)))

(defn
  migrate-directory []
  (.resolve (user-directory) (find-migrate-dir-name)))

(defn
  #^{:doc "Returns the file object if the given file is in the given directory, nil otherwise."}
  find-directory [directory file-name]
  (when-let
    [file (cond (and (string? file-name) (string? directory))
                (Paths/get directory file-name)
                (and (string? file-name) (instance? Path directory))
                (.resolve directory file-name)
                :else nil)]
    (when (Files/exists file (make-array LinkOption 0))
      file)))

(defn
  #^{:doc "Finds the migrate directory."}
  find-migrate-directory []
  (let [user-directory (user-directory)
        migrate-dir-name (find-migrate-dir-name)]
    (find-directory user-directory migrate-dir-name)))

(defn
  migrate-namespace-dir
  ([] (migrate-namespace-dir (find-migrate-dir-name)))
  ([migrate-dir-name]
   (when migrate-dir-name
     (.substring migrate-dir-name (count (config/find-src-dir))))))

(defn
  #^{:doc "Returns the namespace prefix for the migrate directory name."}
  migrate-namespace-prefix-from-directory
  ([] (migrate-namespace-prefix-from-directory (config/find-migrate-dir-name)))
  ([migrate-dir-name]
   (utils/slashes-to-dots (utils/underscores-to-dashes (migrate-namespace-dir migrate-dir-name)))))

(defn
  migrate-namespace-prefix []
  (or (config/namespace-prefix) (migrate-namespace-prefix-from-directory)))

(defn
  #^{:doc "Returns a string for the namespace of the given file in the given directory."}
  namespace-string-for-file [file-name]
  (when file-name
    (str (migrate-namespace-prefix) "." (utils/clj-file-to-symbol-string file-name))))

(defn
  namespace-name-str [migration-namespace]
  (when migration-namespace
    (if (string? migration-namespace)
      migration-namespace
      (name (ns-name migration-namespace)))))

(defn
  migration-namespace? [migration-namespace]
  (.startsWith (namespace-name-str migration-namespace) (str (migrate-namespace-prefix) ".")))

(defn
  migration-number-from-namespace [migration-namespace]
  (when migration-namespace
    (when-let [migration-number-str (re-find #"^[0-9]+" (last (string/split (namespace-name-str migration-namespace) #"\.")))]
      (Long/parseLong migration-number-str))))

(defn migration-comparator [ascending?]
  (reify Comparator
    (compare [_ namespace1 namespace2]
      (try
        (if ascending?
          (.compareTo (migration-number-from-namespace namespace1) (migration-number-from-namespace namespace2))
          (.compareTo (migration-number-from-namespace namespace2) (migration-number-from-namespace namespace1)))
        (catch Throwable t
          (.printStackTrace t))))
    (equals [this object] (= this object))))

(defn user-migration-namespaces []
  (when-let [migration-namespaces (config/migration-namespaces)]
    (migration-namespaces (find-migrate-dir-name) (migrate-namespace-prefix))))

(defn default-migration-namespaces []
  (->> (find-migrate-directory)
       Files/list
       .iterator
       iterator-seq
       (map #(str (.getFileName %)))
       (filter #(re-matches #".*\.clj$" %))
       (map namespace-string-for-file)))

(defn sort-migration-namespaces
  ([migration-namespaces] (sort-migration-namespaces migration-namespaces true))
  ([migration-namespaces ascending?]
   (seq
     (doto (TreeSet. ^Comparator (migration-comparator ascending?))
       (.addAll migration-namespaces)))))

(defn unsorted-migration-namespaces []
  (set (or (user-migration-namespaces) (default-migration-namespaces))))

(defn migration-namespaces
  ([] (migration-namespaces true))
  ([ascending?]
   (sort-migration-namespaces (unsorted-migration-namespaces) ascending?)))

(defn
  #^{:doc "Returns all of the migration file names with numbers between low-number and high-number inclusive."}
  migration-namespaces-in-range [low-number high-number]
  (sort-migration-namespaces
    (filter
      (fn [migration-namespace]
        (let [migration-number (migration-number-from-namespace migration-namespace)]
          (and (>= migration-number low-number) (<= migration-number high-number))))
      (unsorted-migration-namespaces))
    (< low-number high-number)))

(defn
  #^{:doc "Returns all of the numbers prepended to the migration files."}
  migration-numbers
  ([] (migration-numbers (migration-namespaces)))
  ([migration-namespaces]
   (filter identity (map migration-number-from-namespace migration-namespaces))))

(defn max-migration-number
  "Returns the maximum number of all migration files."
  ([migration-namespaces] (reduce max 0 (migration-numbers migration-namespaces)))
  ([] (reduce max 0 (migration-numbers))))

(defn
  #^{:doc "Returns the next number to use for a migration file."}
  find-next-migrate-number []
  (inc (max-migration-number)))

(defn
  #^{:doc "Finds the number of the migration file before the given number"}
  migration-number-before
  ([migration-number] (migration-number-before migration-number (migration-namespaces)))
  ([migration-number migration-namespaces]
   (when migration-number
     (apply max 0 (filter #(< %1 migration-number) (migration-numbers migration-namespaces))))))

(defn
  find-migration-namespace [migration-name]
  (some
    #(when (re-find (re-pattern (str (migrate-namespace-prefix) "\\.[0-9]+-" migration-name)) %1) %1)
    (map namespace-name-str (migration-namespaces))))

(defn
  #^{:doc "The migration file with the given migration name."}
  find-migration-file
  ([migration-name] (find-migration-file (find-migrate-directory) migration-name))
  ([^Path migrate-directory migration-name]
   (when-let [namespace-str (find-migration-namespace migration-name)]
     (.resolve migrate-directory (.getFileName (Paths/get (utils/symbol-string-to-clj-file namespace-str) (make-array String 0)))))))

(defn
  #^{:doc "Returns the migration namespace for the given migration file."}
  migration-namespace [^Path migration-file]
  (when migration-file
    (namespace-string-for-file (-> migration-file (.getFileName) (.toString)))))
