(ns fr.jeremyschoffen.factory.docs.core
  (:require
    [babashka.fs :as fs]
    [clojure.string :as string]
    [fr.jeremyschoffen.prose.alpha.document.clojure :as doc]
    [fr.jeremyschoffen.prose.alpha.eval.common :as evc]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.out.markdown.compiler :as cplr]))


;; -----------------------------------------------------------------------------
;; Setting up docs evaluation
;; -----------------------------------------------------------------------------
(def doc-root "resources/docs/")
(def spit-root "doc/")


(def eval-doc (doc/make-evaluator {:slurp-doc slurp
                                   :read-doc reader/read-from-string
                                   :eval-forms evc/eval-forms-in-temp-ns}))

;; -----------------------------------------------------------------------------
;; Paths utilities
;; -----------------------------------------------------------------------------

(defn src-path [path]
  (str doc-root path))


(defn strip-ext [file-name]
  (string/replace-first file-name ".prose" ""))


(defn dest-path [path]
  (->> path
       strip-ext
       (str spit-root)))


(defn compile-doc [path]
  (-> path
      eval-doc
      cplr/compile!))


;; -----------------------------------------------------------------------------
;; Generating docs
;; -----------------------------------------------------------------------------
(defn readme! []
  (spit "README.md"
        (-> "readme.md.prose"
            src-path
            compile-doc)))


(defn ensure-parents! [path]
  (when-let [p (fs/parent path)]
    (fs/create-dirs p))
  path)


(defn spit! [dest & args]
  (apply spit (ensure-parents! dest) args))


(defn compile-doc! [path]
  (spit! (dest-path path)
         (-> path src-path compile-doc)))


(comment
  (readme!)
  (compile-doc! "v1/computations.md.prose")
  (compile-doc! "v2/factories.md.prose"))

