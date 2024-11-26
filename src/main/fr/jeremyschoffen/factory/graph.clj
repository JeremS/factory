(ns fr.jeremyschoffen.factory.graph
  (:require
    [fr.jeremyschoffen.factory.graph.mini-loom :as g]
    [hyperfiddle.rcf :refer [tests]]))



(def digraph g/digraph)
(def topsort g/topsort)
(def edges g/edges)

;; -----------------------------------------------------------------------------
;; Graph utils
;; -----------------------------------------------------------------------------
(defn predecessors
  "Return a set of all transitive predecessors of a `node` including the `node`
  passed in parameter"
  [graph node]
  (let [ps (g/predecessors graph)]
    (set (g/pre-traverse ps node))))


(defn successors [graph node]
  (let [ss (g/successors graph)]
    (set (g/pre-traverse ss node))))


(defn transitive-deps
  "Return a set of all transitive predecessors of a set of `nodes`. The
  `nodes` passed in parameter are included in the resulting set"
  [graph nodes]
  (let [nodes (set nodes)]
    (loop [seen #{}
           [node & rest-nodes] nodes]
      (cond
        (not node) seen

        (contains? seen node)
        (recur seen rest-nodes)

        :else
        (recur (into seen (predecessors graph node))
               rest-nodes)))))


(tests
  (def ex-graph
    (g/digraph
      [:a :b]
      [:a :c]
      [:b :d]
      [:c :d]
      [:d :e]
      [:a :b']
      [:b' :c']
      [:c' :d']))

  (predecessors ex-graph :c) := #{:c :a}
  (predecessors ex-graph :e) := #{:a :b :c :d :e}
  (transitive-deps ex-graph #{:c :d'}) := #{:c' :b' :c :d' :a}
  (successors ex-graph :a) := (g/nodes ex-graph))



