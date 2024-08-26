(ns user
  (:require
    [clj-reload.core :as r]
    [cljs.repl.node :as node]
    [cider.piggieback :as piggie]
    [hyperfiddle.rcf]))

(hyperfiddle.rcf/enable!)


(defn start-node-repl []
  (piggie/cljs-repl (node/repl-env)))


(r/init
  {:no-reload #{'user}})


(defn reload! []
  (r/reload))


(comment
  (reload!)
  (start-node-repl)
  (type 1)
  :cljs/quit)
