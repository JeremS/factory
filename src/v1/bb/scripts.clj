(ns scripts
  (:require
    [babashka.tasks :as t]))


(defn aliases->str [aliases]
  (apply str aliases))


;; -----------------------------------------------------------------------------
;; Reveal task
;; -----------------------------------------------------------------------------
(def reveal-nrepl-middleware
  '[cider.piggieback/wrap-cljs-repl
    vlaaad.reveal.nrepl/middleware])


(def reveal-aliases
  #{:clj
    :cljs
    :dev
    :nrepl
    :piggie
    :reveal})


(defn reveal []
  (let [cmd (format "-M%s -m nrepl.cmdline --middleware %s"
                    (aliases->str reveal-aliases)
                    reveal-nrepl-middleware)]
    (t/clojure (format "-M%s -m nrepl.cmdline --middleware \"%s\""
                 (aliases->str reveal-aliases)
                 reveal-nrepl-middleware)))) 


