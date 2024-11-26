(ns fr.jeremyschoffen.factory.utils.notebooks
  (:require
    [fr.jeremyschoffen.factory.graph :as g]))



(defn graph->mermaid
  "Turns a graph into a mermaid flowchart string"
  [g]
  (let [buf (StringBuffer.)]
    (.append buf "flowchart TD\n")
    (doseq [[from to] (g/edges g)]
      (.append buf from)
      (.append buf "-->")
      (.append buf to)
      (.append buf "\n"))
    (.toString buf)))


