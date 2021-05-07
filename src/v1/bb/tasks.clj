(ns tasks
  (:require
    [babashka.tasks :as t]))


(defn aliases->str [aliases]
  (apply str aliases))


(defn repl-cmd-args [aliases middleware]
  (format "-M%s -m nrepl.cmdline --middleware \"%s\""
    (aliases->str aliases)
    middleware))


;; -----------------------------------------------------------------------------
;; Repl task
;; -----------------------------------------------------------------------------
(def repl-nrepl-middleware
  '[cider.piggieback/wrap-cljs-repl])


(def repl-nrepl-aliases
  #{:clj
    :cljs
    :dev
    :nrepl
    :piggie
    :test})

(defn repl []
  (t/clojure (repl-cmd-args repl-nrepl-aliases
                            repl-nrepl-middleware)))


;; -----------------------------------------------------------------------------
;; Reveal task
;; -----------------------------------------------------------------------------
(def reveal-nrepl-middleware
  (conj repl-nrepl-middleware
        'vlaaad.reveal.nrepl/middleware))


(def reveal-nrepl-aliases
  (conj repl-nrepl-aliases
        :reveal))


(defn reveal []
  (t/clojure (repl-cmd-args reveal-nrepl-aliases
                            reveal-nrepl-middleware)))


;; -----------------------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------------------
(defn test-cmd [id]
  (format "-M:clj:cljs:test -m kaocha.runner %s"
          id))


(defn test-clj []
  (t/clojure (test-cmd :unit-clj)))


(defn test-cljs []
  (t/clojure (test-cmd :unit-cljs)))
