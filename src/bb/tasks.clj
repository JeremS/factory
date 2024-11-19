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
  '[cider.nrepl/cider-middleware
    cider.piggieback/wrap-cljs-repl])


(def repl-nrepl-aliases
  #{:clj
    :cljs
    :dev
    :docs
    :nrepl
    :piggie
    :test})


(defn repl []
  (clojure (repl-cmd-args repl-nrepl-aliases
                          repl-nrepl-middleware)))

(defn debug []
  (clojure (repl-cmd-args (-> repl-nrepl-aliases (disj :clj) (conj :debug))
                          repl-nrepl-middleware)))
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

