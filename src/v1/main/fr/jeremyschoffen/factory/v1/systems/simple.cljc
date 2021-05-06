(ns fr.jeremyschoffen.factory.v1.systems.simple
  (:require
    [clojure.set :as s]
    [loom.graph :as loom]
    [meander.epsilon :as m]
    [medley.core :as medley]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.systems.protocols :as p]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


(extend-type #?(:clj Object :cljs default)
  p/Dependent
  (dependent? [this] false)
  (dependencies [this] #{})

  p/Computation
  (computation? [this] false)
  (compute [this] nil))

(def simple-impl
  {`p/dependent? (constantly true)
   `p/dependencies (fn [this] 
                     (println "Yo " this)
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
  (vary-meta m merge simple-impl))


(comment
  (def ex1 (c {:deps #{:a :b}}))
  (p/dependencies ex1))


(defn wrap-aliases [m alias-map]
  (vary-meta m update 'p/compute
             (fn [compute-fn]
               (fn [this deps]
                 (compute-fn this (s/rename-keys deps alias-map))))))




(defn classify [v]
  (if (p/computation? v)
    ::computation-map
    ::initial-state))


(defn edges-for-computation [computation-name deps]
  (-> deps
    p/dependencies
    (m/rewrite 
      (m/seqable !parent-name ...)
      [[!parent-name ~computation-name] ...])))



(defn edges-for-computations-map [m]
  (mapcat #(apply edges-for-computation %) m))


(comment
  (def ex2 {:x (c {:deps #{:a :b}})
            :y (c {:deps #{:c :x}})})

  (edges-for-computations-map ex2))


(defn make-graph [computation-map]
  (apply loom/digraph
         (edges-for-computations-map computation-map)))


(defn system [spec-map]
  (let [{::keys [initial-state computation-map] :as s} (u/split-map spec-map classify)
        computation-names (-> computation-map keys set)
        graph (make-graph computation-map)
        order (->> graph 
                g/topsort
                (filterv computation-names))]
    (-> {::initial-state initial-state
         ::computation-names computation-names
         ::dependency-graph graph
         ::total-order order})))




(comment
  (do
    (defn apply-taxe [{:keys [v t]}]
      (+ v
         (* v t)))

    (def ex
      {:total-bt 100
       :taxe 0.22
       :total (wrap-aliases
                (c {:deps #{:total-bt :taxe}
                    :f apply-taxe})
                {:total-bt :v
                 :taxe :t})}))
  (system ex))
