(ns fr.jeremyschoffen.factory.v1.computations.building-blocks
  (:require
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]
    [fr.jeremyschoffen.factory.v1.utils :as u]))

;; -----------------------------------------------------------------------------
;; System info
;; -----------------------------------------------------------------------------
(defn computation-names [computations]
  (-> computations keys set))


(defn dependency-graph [computations]
  (g/make-graph computations))


(defn computations-order [dependency-graph computation-names]
  (->> dependency-graph
    g/topsort
    (filterv (set computation-names))))


;; -----------------------------------------------------------------------------
;; Promise based async/parallel computation
;; -----------------------------------------------------------------------------
(defn make-combine-map [{:keys [combine then]}]
  (fn combine-map [m]
    (let [[names promises] (m/find m
                             (m/map-of !k !v)
                             [!k !v])]
      (-> (combine promises)
          (then (fn [ps]
                  (zipmap names ps)))))))


(defn make-combine-mixed-map [{:keys [combine-map promise? then]}]
  (fn combine-mixed-map [m]
    (let [{realized-deps false
           deferred-deps true} (u/split-map m promise?)]
      (if (empty? deferred-deps)
        realized-deps
        (-> deferred-deps
            combine-map
            (then (fn [newly-realized-deps]
                    (merge realized-deps newly-realized-deps))))))))


(defn make-gather-deps-async [{:keys [combine-mixed-map gather-deps]}]
  (fn gather-deps-async [state deps-names]
    (-> state
        (gather-deps deps-names)
        combine-mixed-map)))


(defn make-compute-async [{:keys [compute promise? then]}]
  (fn compute-async [computation current-val deps]
    (if-not (promise? deps)
      (compute computation current-val deps)
      (-> deps
          (then (fn [realized-deps]
                  (compute computation current-val realized-deps)))))))

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


(defn make-run [{:keys [execute-computations split-config]}]
  (fn run [config]
    (let [{:keys [inputs computations]} (split-config config)
          computation-names (computation-names computations)
          dependency-graph (dependency-graph computations)
          order (computations-order dependency-graph computation-names)]
      (execute-computations inputs computations order))))
