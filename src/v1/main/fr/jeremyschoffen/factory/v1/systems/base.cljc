(ns fr.jeremyschoffen.factory.v1.systems.base
  (:require
    [clojure.set :as s]
    [loom.graph :as loom]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.systems.protocols :as p]))


(def impl
  {`p/dependent? (constantly true)
   `p/dependencies (fn [this]
                     (:deps this))
   `p/computation? (constantly true)
   `p/compute (fn [this deps]
                ((:f this) deps))})


(defn c
  "Define a computation from a map.

  key args:
  - `:deps`: names of the dependencies
  - `f`: the computation, a function 'deps -> result'. `deps` is a map of dependencies.
  "
  [m]
  (-> m
    (update :deps set)
    (vary-meta merge impl)))


(defn wrap-fn-with-aliases [f aliases]
  (fn [deps-map]
    (f (s/rename-keys deps-map aliases))))


(defn- wrap-compute-fn-with-aliases [f aliases]
  (fn [this deps]
    (f this (s/rename-keys deps aliases))))


(defn wrap-computation-with-aliases [computation-map aliases]
  (vary-meta computation-map update `p/compute wrap-compute-fn-with-aliases aliases))


(defn classify [v]
  (if (p/computation? v)
    ::computations-map
    ::initial-state))


(defn edges-for-computation [computation-name deps]
  (-> deps
    p/dependencies
    (m/rewrite
      (m/seqable !parent-name ...)
      [[!parent-name ~computation-name] ...])))


(defn edges-for-computations-map [m]
  (mapcat #(apply edges-for-computation %) m))


(defn make-graph [computation-map]
  (apply loom/digraph
         (edges-for-computations-map computation-map)))


(defn add-current-val [m v]
  (vary-meta m assoc ::current-val v))


(defn gather-deps [state deps computation-name]
  (-> state
      (select-keys deps)
      (add-current-val (get state computation-name))))


(defn current-val [deps-map]
  (-> deps-map meta ::current-val))


(defn execute-computation [state computations-map computation-name]
  (let [computation (get computations-map computation-name)
        deps (p/dependencies computation)
        deps-map (gather-deps state deps computation-name)]
    (p/compute computation deps-map)))


(defn execute-computations [state computations-map computation-names]
  (persistent!
   (reduce
     (fn [acc computation-name]
       (let [r (execute-computation acc computations-map computation-name)]
         (assoc! acc computation-name r)))
     (transient state)
     computation-names)))


(defn system [computations-map]
  (let [graph (make-graph computations-map)
        computation-names (-> computations-map keys set)
        input-names (s/difference (loom/nodes graph)
                                  computation-names)
        order (->> graph
                g/topsort
                (filterv computation-names))]
    {::computations-map computations-map
     ::input-names input-names
     ::computation-names computation-names
     ::dependency-graph graph
     ::total-order order}))
