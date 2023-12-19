(ns drift.utils
  (:require [clojure.string :as string]))

(def file-separator
  (.getProperty (System/getProperties) "file.separator"))

(defn dots-to-slashes [s]
  (when s (string/replace s "." file-separator)))

(defn slashes-to-dots [s]
  (when s (string/replace s #"[/\\]" ".")))

(defn underscores-to-dashes [s]
  (when s (string/replace s "_" "-")))

(defn dashes-to-underscores [s]
  (when s (string/replace s "-" "_")))

(defn strip-clj-file-ending [name]
  (if (and name (.endsWith name ".clj"))
    (subs name 0 (- (count name) 4))
    name))

(defn clj-file-to-symbol-string [name]
  (-> name
      strip-clj-file-ending
      underscores-to-dashes
      slashes-to-dots))

(defn symbol-string-to-clj-file [name]
  (when-let [name-without-file-ending (-> name
                                          dashes-to-underscores
                                          dots-to-slashes)]
    (println name-without-file-ending)
    (str name-without-file-ending ".clj")))
