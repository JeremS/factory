(ns fr.jeremyschoffen.factory.v1.dependencies.graph
  (:require
    [clojure.set :as s]
    [loom.graph :as loom]
    [loom.alg :as loom-alg]
    [loom.alg-generic :as loom-alg-g]
    [loom.derived :as loom-d]))


(defn topsort
  "Get a topological order for a graph. Throws when there is a cycle."
  [graph]
  (if-let [res (loom-alg/topsort graph)]
    res
    (throw (ex-info "Cycle in the computation graph"
                    {:graph graph}))))

(comment
  (do
    (def g (loom/digraph
             [:a :x]
             [:b :x]
             [:c :y]
             [:x :y]))

    (def cyclical-g
      (loom/digraph
        [:a :x]
        [:b :x]
        [:c :y]
        [:y :a]
        [:x :y]))

    (require '[loom.io :as lio]))

  (lio/view g)
  (topsort g)
  (topsort cyclical-g))


(defn reachable-from-nodes
  "Return all nodes of a graph that are reachable from a set of `nodes`
  using the `neighbors` function. The original set `nodes` is included in the result.
  
  Args:
  - `nodes`: A set of node names
  - `neighbors`: a function node -> neighbors"
  [neighbors nodes]
  (let [nodes (set nodes)]
    (loop [result #{}
           unseen nodes]
      (if-not (seq unseen)
        result
        (let [current (first unseen)
              reachable (loom-alg-g/pre-traverse neighbors current :seen result)]
          (println "current" current)
          (println "reachable" reachable)
          (recur
            (into result reachable)
            (rest unseen)))))))


(defn reachable-from-node
  "Return all nodes of a graph that are reachable from a `node`
  using the `neighbors` function. The original `node` is included in the result.
  
  Args:
  - `node`: The name of a node used a a starting point
  - `neighbors`: a function node -> neighbors"
  [neighbors node]
  (reachable-from-nodes neighbors #{node}))



(comment
  (reachable-from-node (loom/predecessors g) :x)
  (reachable-from-node (loom/predecessors g) :y)
  (reachable-from-node (loom/successors g) :c)
  (reachable-from-node (loom/successors g) :a))
