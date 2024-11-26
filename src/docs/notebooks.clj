(ns notebooks
  (:require
    [scicloj.clay.v2.api :as clay]))


(defn export! []
  (clay/make! {:source-path "src/docs/notebooks/graph_example.clj"
               :format [:html]
               :live-reload false}))


(comment
  (export!))

(defn md-link
  ([text]
   (format "[%s]()"text))
  ([text url]
   (format "[%s](%s)"text url)))



(def plumatic-graph
  (md-link "plumatic graph"
           "https://github.com/plumatic/plumbing"))
