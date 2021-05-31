(ns ^{:author "Jeremy Schoffen"
      :doc "
Collection of utilities used to create, analyse and manipulate dependency graphs.
      "}
  fr.jeremyschoffen.factory.v1.dependencies.graph
  (:require
    [loom.graph :as loom]
    [loom.alg :as loom-alg]
    [loom.alg-generic :as loom-alg-g]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]))


;; -----------------------------------------------------------------------------
;; Graph creation
;; -----------------------------------------------------------------------------
(defn edges-for-dependent
  "Given a dependent and its name, return a vector of edges used to create a
  dependency graph."
  [dependent-name dependent]
  (-> dependent
    p/dependencies
    (m/rewrite
      (m/seqable !parent-name ...)
      [[!parent-name ~dependent-name] ...])))


(defn edges-for-computations-map
  "Given a map of dependents, return a vector of edges used to create a
  dependency graph."
  [m]
  (mapcat #(apply edges-for-dependent %) m))


(defn make-graph
  "Given a map of dependents return a dependency graph."
  [computation-map]
  (apply loom/digraph
         (edges-for-computations-map computation-map)))

;; -----------------------------------------------------------------------------
;; Utils
;; -----------------------------------------------------------------------------
(def ^{:arglists '([g])} nodes
  "Return a collection the nodes in a graph."
  loom/nodes)


(def ^{:arglists '([g])} edges
  "Return a collection the edges in a graph."
  loom/edges)


(def ^{:arglists '([g] [g node-name])} successors
  "Return the successors of `node-name` in the graph `g` or a partial application of this function."
  loom/successors)


(def ^{:arglists '([g] [g node-name])} predecessors
  "Return the successors of `node-name` in the graph `g` or a partial application of this function."
  loom/predecessors)


(defn starting-point?
  "Checks wether or not a node is a starting point in the graph, aka a node without dependencies."
  [dependency-graph node-name]
  (empty? (predecessors dependency-graph node-name)))


(defn starting-points
  "Get a set of all sarting points in a graph."
  [dependency-graph]
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

