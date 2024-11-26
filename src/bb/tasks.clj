(ns tasks
  (:require
    [clojure.string :as string]
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
  '[cider.nrepl/cider-middleware])


(def repl-nrepl-aliases
  #{:clj
    :dev
    :additional-libs
    :docs
    :nrepl
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
(def test-aliases
  [:clj :additional-libs  :test])

(def test-dir "src/test")


(def test-invocation
  (-> (format "-M%s -m lazytest.main -d %s"
              (aliases->str test-aliases)
              test-dir)
      (cons *command-line-args*)
      (->> (string/join " "))))


(defn run-tests [& _]
  (clojure test-invocation))



;; -----------------------------------------------------------------------------
;; Kondo
;; -----------------------------------------------------------------------------

(defn kondo []
  (-> (p/sh ["clj-kondo" "--lint" "src/v1/main" "--lint" "src/v1/test/"])
    :out
    println))

