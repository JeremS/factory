(ns notebooks.graphs
  (:require
    [fr.jeremyschoffen.factory.v2.core :as f]
    [fr.jeremyschoffen.factory.v2.utils.clerk :as uc]
    [nextjournal.clerk :as clerk]))


(def factory
  {:bb1 (f/bb identity :a :b)
   :bb2 (f/bb identity :c :bb1)})


(clerk/with-viewer uc/mermaid-viewer
  (-> factory
      f/factory->graph
      uc/graph->mermaid))

