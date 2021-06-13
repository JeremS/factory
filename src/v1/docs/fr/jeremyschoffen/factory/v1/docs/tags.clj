(ns fr.jeremyschoffen.factory.v1.docs.tags
  (:require
    [clojure.string :as string]
    [clojure.repl]
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]
    [fr.jeremyschoffen.prose.alpha.out.markdown.tags :as md]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]))


(defn make-link [href text]
  (lib/xml-tag :a {:href href} text))


(def graph (make-link "https://github.com/plumatic/plumbing" "graph"))


;; -----------------------------------------------------------------------------
;; Code samples.
;; -----------------------------------------------------------------------------
(defn clj [& args]
  (apply md/code-block {:content-type "clojure"} args))


(defmacro code [& body]
  (let [body (string/join body)
        read-code (-> body
                    (as-> s (str "(do " s ")"))
                    (reader/read-string*))]
    (lib/<>
      (clj body)
      "\n;=>\n"
      (clj read-code))))


(defmacro code-s [& body]
  (let [body (string/join body)
        read-code (-> body
                    (as-> s (str "(do " s ")"))
                    (reader/read-string*))]
    (lib/<>
      (md/code-block {:content-type "clojure"} body)
      `(do ~read-code ""))))


(defmacro source [sym]
  `(clj
     (with-out-str
       (clojure.repl/source ~sym))))



(defn ns-qualify-ish [s]
  (if-let [n (some-> s namespace symbol)]
    (or (some-> (get (ns-aliases *ns*) n) str (symbol (name s)))
        s)
    (or (some-> (get (ns-aliases *ns*) s) str symbol)
        (symbol (-> *ns* .getName str) (name s)))))



(defmacro sym [s]
  `(str "`" ~(str (ns-qualify-ish s) "`")))

