(ns user
  (:require
    [nextjournal.clerk :as clerk]
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
  (clerk/serve! {:watch-paths ["src/dev/notebooks"]})
  (reload!)
  (start-node-repl)
  (type 1)
  :cljs/quit)
