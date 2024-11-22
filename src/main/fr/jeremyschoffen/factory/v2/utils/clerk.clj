(ns fr.jeremyschoffen.factory.v2.utils.clerk
  (:require
    [loom.graph :as g]
    [nextjournal.clerk :as clerk]))

;; Taken from https://book.clerk.vision/#loading-libraries
(def mermaid-viewer
  {:transform-fn clerk/mark-presented
   :render-fn '(fn [value]
                 (when value
                   [nextjournal.clerk.render/with-d3-require {:package ["mermaid@8.14/dist/mermaid.js"]}
                    (fn [mermaid]
                      [:div {:ref (fn [el] (when el
                                             (.render mermaid (str (gensym)) value #(set! (.-innerHTML el) %))))}])]))})


(defn graph->mermaid [g]
  (let [buf (StringBuffer.)]
    (.append buf "flowchart TD\n")
    (doseq [[from to] (g/edges g)]
      (.append buf from)
      (.append buf "-->")
      (.append buf to)
      (.append buf "\n"))
    (.toString buf)))


