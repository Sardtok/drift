(ns drift.builder
  (:require [clojure.tools.logging :as logging]
            [drift.config :as config]
            [drift.core :as core]
            [drift.utils :as utils])
  (:import (java.nio.file Files Paths Path LinkOption)
           (java.nio.file.attribute FileAttribute)
           (java.text SimpleDateFormat)
           (java.util Date)))

(defn
  #^{:doc "Finds or creates if missing, the migrate directory in the given db directory."}
  find-or-create-migrate-directory
  ([] (find-or-create-migrate-directory (core/migrate-directory)))
  ([migrate-directory]
   (when migrate-directory
     (if (Files/exists migrate-directory (make-array LinkOption 0))
       (logging/info "Migrate directory already exists.")
       (do
         (logging/info "Creating migrate directory...")
         (.mkdirs migrate-directory)))
     migrate-directory)))

(defn incremental-migration-number-generator []
  (format "%03d" (core/find-next-migrate-number)))

(defn timestamp-migration-number-generator []
  (.format (SimpleDateFormat. "yyyyMMddHHmmss") (new Date)))

(defn migration-number-generator-fn []
  (or (config/migration-number-generator) timestamp-migration-number-generator))

(defn migration-number []
  ((migration-number-generator-fn)))

(defn
  #^{:doc "Creates a new migration file from the given migration name."}
  create-migration-file
  ([migration-name] (create-migration-file (find-or-create-migrate-directory) migration-name))
  ([migrate-directory migration-name]
   (if (and migrate-directory migration-name)
     (let [migration-file-name (str (migration-number) "_" (utils/dashes-to-underscores migration-name) ".clj")
           migration-file (.resolve migrate-directory migration-file-name)]
       (logging/info (str "Creating migration file " migration-file-name "..."))
       (Files/createFile migration-file (make-array FileAttribute 0))
       migration-file))))
