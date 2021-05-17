(ns fr.jeremyschoffen.factory.v1.computations.building-blocks
  (:require
    [clojure.set :as s]
    [loom.graph :as loom]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]))

;; -----------------------------------------------------------------------------
;; System info
;; -----------------------------------------------------------------------------
(defn computation-names [{:keys [computations]}]
  (-> computations keys set))


(defn dependency-graph [{:keys [computations]}]
  (g/make-graph computations))


(defn computations-order [{:keys [dependency-graph computation-names]}]
  (->> dependency-graph
    g/topsort
    (filterv (set computation-names))))


(defn input-names [{:keys [dependency-graph computation-names]}]
  (let [nodes (-> dependency-graph loom/nodes set)]
    (s/difference nodes computation-names)))


;; -----------------------------------------------------------------------------
;; Execution fns
;; -----------------------------------------------------------------------------
(defn make-execute-computation [{:keys [gather-deps compute]}]
  (fn execute-computation [inputs computation-name computation]
    (let [dep-names (p/dependencies computation)
          deps (gather-deps inputs dep-names)
          current-val (get inputs computation-name)]
      (compute computation current-val deps))))


(defn make-execute-computations [{:keys [execute-computation]}]
  (fn execute-computations [inputs computations order]
    (persistent!
      (reduce
        (fn [state computation-name]
          (assoc! state computation-name
            (execute-computation state
                                 computation-name
                                 (get computations computation-name))))
        (transient inputs)
        order))))

