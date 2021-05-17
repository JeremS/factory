(ns tasks
  (:require
    [babashka.tasks :as t]
    [babashka.process :as p]))


(defn aliases->str [aliases]
  (apply str aliases))


(defn repl-cmd-args [aliases middleware]
  (format "-M%s -m nrepl.cmdline --middleware \"%s\""
    (aliases->str aliases)
    middleware))


(defn clojure [cmd-string-arg]
  (println "Calling clojure: " cmd-string-arg)
  (t/clojure cmd-string-arg))

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
  (clojure (repl-cmd-args repl-nrepl-aliases
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
  (clojure (repl-cmd-args reveal-nrepl-aliases
                            reveal-nrepl-middleware)))


;; -----------------------------------------------------------------------------
;; Tests
;; -----------------------------------------------------------------------------
(defn test-cmd [id]
  (format "-M:clj:cljs:test -m kaocha.runner %s"
          id))


(defn test-clj []
  (clojure (test-cmd :unit-clj)))


(defn test-cljs []
  (clojure (test-cmd :unit-cljs)))


;; -----------------------------------------------------------------------------
;; Kondo
;; -----------------------------------------------------------------------------

(defn kondo []
  (-> (p/sh ["clj-kondo" "--lint" "src/v1/main" "--lint" "src/v1/test/"])
    :out
    println))

