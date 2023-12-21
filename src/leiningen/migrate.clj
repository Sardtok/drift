(ns leiningen.migrate
  "Run drift migration scripts."
  (:require [drift.drift-version :as drift-version]
            [leiningen.core.eval :refer [eval-in-project]]))

(defn migrate [project & args]
  "Run migration scripts."
  (let [drift-config (-> project
                         :drift-config
                         (#(when % (symbol %))))]
    (eval-in-project
      (update-in project [:dependencies]
                 conj ['com.sigmund-hansen/drift drift-version/version])
      `(drift.execute/run '~drift-config '~args)
      '(require 'drift.execute))))
