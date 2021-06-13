(ns fr.jeremyschoffen.factory.v1.docs.core.clj
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [fr.jeremyschoffen.prose.alpha.document.clojure :as doc]
    [fr.jeremyschoffen.prose.alpha.eval.common :as evc]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.out.markdown.compiler :as cplr]))

;; -----------------------------------------------------------------------------
;; Setting up docs evaluation
;; -----------------------------------------------------------------------------
(def doc-root "fr/jeremyschoffen/factory/v1/docs/pages/")


(defn slurp-page [path]
  (->> path
      (str doc-root)
      io/resource
      slurp))


(def eval-doc (doc/make-evaluator {:slurp-doc slurp-page
                                   :read-doc reader/read-from-string
                                   :eval-forms evc/eval-forms-in-temp-ns}))

(defn doc [path]
  (-> path eval-doc cplr/compile!))

;; -----------------------------------------------------------------------------
;; Generating the readme
;; -----------------------------------------------------------------------------
(defn gen-readme! []
  (spit "README.md" (doc "readme.md.prose")))

;; -----------------------------------------------------------------------------
;; Generating docs
;; -----------------------------------------------------------------------------
(defn strip-ext [file-name]
  (string/replace-first file-name ".prose" ""))


(defn compile-doc! [path]
  (let [dest (str "doc/" (strip-ext path))]
   (spit dest (doc path))))


(comment
  (-> "computations.md.prose" slurp-page reader/read-from-string println)
  (gen-readme!)
  (compile-doc! "computations.md.prose"))

