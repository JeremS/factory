(ns user
  (:require
    [clj-reload.core :as r]
    [hyperfiddle.rcf]))


(hyperfiddle.rcf/enable!)


(r/init
  {:no-reload #{'user}})


(defn reload! []
  (r/reload))


(comment
  (require '[nextjournal.clerk :as clerk])
  (clerk/serve! {:watch-paths ["src/dev/notebooks"]})
  (reload!))
