(ns fr.jeremyschoffen.factory.v1.dependencies.graph
  (:require
    [loom.graph :as loom]
    [loom.alg :as loom-alg]
    [loom.alg-generic :as loom-alg-g]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]))

;; -----------------------------------------------------------------------------
;; Graph creation
;; -----------------------------------------------------------------------------
(defn edges-for-computation [computation-name computation]
  (-> computation
    p/dependencies
    (m/rewrite
      (m/seqable !parent-name ...)
      [[!parent-name ~computation-name] ...])))


(defn edges-for-computations-map [m]
  (mapcat #(apply edges-for-computation %) m))


(defn make-graph [computation-map]
  (apply loom/digraph
         (edges-for-computations-map computation-map)))


;; -----------------------------------------------------------------------------
;; Utils
;; -----------------------------------------------------------------------------
(def nodes loom/nodes)


(def edges loom/edges)


(def successors loom/successors)


(def predecessors loom/predecessors)


(defn starting-point? [dependency-graph node-name]
  (empty? (predecessors dependency-graph node-name)))


(defn starting-points [dependency-graph]
  (into #{}
        (filter (partial starting-point? dependency-graph))
        (nodes dependency-graph)))

;; -----------------------------------------------------------------------------
;; Algorithms
;; -----------------------------------------------------------------------------
(defn topsort
  "Get a topological order for a graph. Throws when there is a cycle."
  [graph]
  (if-let [res (loom-alg/topsort graph)]
    res
    (throw (ex-info "Cycle in the computation graph"
                    {:graph graph}))))


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

