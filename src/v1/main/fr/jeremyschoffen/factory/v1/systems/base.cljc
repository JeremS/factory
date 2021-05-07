(ns fr.jeremyschoffen.factory.v1.systems.base
  (:require
    [loom.graph :as loom]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.systems.protocols :as p]
    [fr.jeremyschoffen.factory.v1.systems.impl :as i]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


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


(comment
  (def ex1 (c {:deps #{:a :b}}))
  (p/dependencies ex1))


(defn wrap-compute-fn-with-aliases [f aliases]
  (fn [this deps]
    (f this (s/rename-keys deps aliases))))


(defn wrap-computation-with-aliases [computation-map aliases]
  (vary-meta computation-map update `p/compute wrap-compute-fn-with-aliases aliases))


(comment
  (def ex
    (c {:deps [:total :taxe-coef]
        :f (fn [{:keys [v c]}]
             (+ v (* v c)))}))

  (p/compute ex {:v 100 :c 0.2})

  (p/compute
    (wrap-computation-with-aliases ex {:total :v :taxe-coef :c})
    {:total 100 :taxe-coef 0.2}))


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


(comment
  (def ex2 {:x (i/c {:deps #{:a :b}})
            :y (i/c {:deps #{:c :x}})})

  (edges-for-computations-map ex2))


(defn make-graph [computation-map]
  (apply loom/digraph
         (edges-for-computations-map computation-map)))


(defn system [spec-map]
  (let [{::keys [initial-state computations-map] :as s} (u/split-map spec-map classify)
        computation-names (-> computations-map keys set)
        graph (make-graph computations-map)
        order (->> graph 
                g/topsort
                (filterv computation-names))]
    (-> s
        (assoc
          ::input-names (-> initial-state keys set)
          ::computations-map computations-map
          ::computation-names computation-names
          ::dependency-graph graph
          ::total-order order))))


(defn get-deps [state deps computation-name]
  (-> state
      (select-keys deps)
      (vary-meta assoc ::this (get state computation-name))))


(defn this [deps-map]
  (-> deps-map meta ::this))


(defn execute-computation [state computations-map computation-name]
  (let [computation (get computations-map computation-name)
        deps (p/dependencies computation)
        deps-map (get-deps state deps computation-name)]
    (p/compute computation deps-map)))



(defn execute-computations [state computations-map computation-names]
  (let [res (volatile! (transient {}))]
    {:new-state
     (persistent!
       (reduce
         (fn [acc computation-name]
           (let [r (execute-computation acc computations-map computation-name)]
             (vswap! res assoc! computation-name r)
             (assoc! acc computation-name r)))
         (transient state)
         computation-names))
     :res (persistent! @res)}))


(comment
  (do
    (defn apply-taxe [{:keys [v c]}]
      (+ v
         (* v c)))


    (def total-component
      (i/c {:deps #{:total-bt :taxe}
            :f apply-taxe}))


    (def ex
      {:total-bt 100
       :fixed-overhead 10
       :taxe 0.22
       :sub-total (i/wrap-computation-with-aliases
                    total-component
                    {:total-bt :v
                     :taxe :c})
       :total (i/c {:deps [:fixed-overhead :sub-total]
                    :f (fn [deps]
                         (->> deps
                              vals
                              (apply +)))})}))

  (execute-computation
    {:v 100 :c 0.3}
    {:total (i/c {:deps [:v :c]
                  :f apply-taxe})}
    :total)

  (execute-computation ex ex :sub-total)

  (execute-computations ex ex [:sub-total :total])

  (::total-order (system ex))
  (let [{::keys [initial-state computations-map total-order]} (system ex)]
    (execute-computations initial-state computations-map total-order)))
