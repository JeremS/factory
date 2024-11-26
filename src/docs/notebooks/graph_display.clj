(ns notebooks.graph-display
  (:require
    [fr.jeremyschoffen.factory.core :as c]
    [fr.jeremyschoffen.factory.utils.notebooks :as u]
    [notebooks.graph-example :as ex]
    [scicloj.kindly.v4.kind :as kind]))

;; Lets display this factory:
ex/stats-factory



;; We add mermaid to the page
(kind/hiccup
  [:script
   {:type "module"}
   " import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
   mermaid.initialize({ startOnLoad: true}); "])


;; We can then turn the factory into a graph and
;; turn this graph into a mermaid string:

(kind/hiccup
  [:pre.mermaid
    (-> ex/stats-factory
        c/factory->graph
        u/graph->mermaid)])

^:kindly/hide-code
(comment
  (require '[scicloj.clay.v2.api :as clay])
  (clay/make! {:source-path "src/docs/notebooks/graph_display.clj"
               :live-reload true}))
