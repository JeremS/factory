(ns fr.jeremyschoffen.factory.v1.systems.common
 (:require
   [meander.epsilon :as m]
   [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]
   [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]))

;; -----------------------------------------------------------------------------
;; Config -> system
;; -----------------------------------------------------------------------------
(defn components->triples [components]
  (m/search components
    {?component-name {?phase-name ?computation}}
    [?component-name ?phase-name ?computation]))


(defn- triple->transient-computations! [triples]
  (persistent!
   (reduce
     (fn [acc [component-name phase-name computation]]
       (let [current-phase (get acc phase-name (transient {}))]
         (assoc! acc phase-name (assoc! current-phase component-name computation))))
     (transient {})
     triples)))


(defn- persist! [m]
  (persistent!
    (reduce-kv
      (fn [acc k v]
        (assoc! acc k (persistent! v)))
      (transient {})
      m)))


(defn triples->computations-maps [triples]
  (->> triples
    triple->transient-computations!
    persist!))


(defn components->computations-maps [components]
  (-> components components->triples triples->computations-maps))


(defn computations->phase [computations]
  {:computations computations
   :computation-names (bb/computation-names computations)})


(defn computations-maps->phases [phases]
  (reduce-kv
    (fn [acc k v]
      (assoc acc k (computations->phase v)))
    {}
    phases))


(defn components->phases [components]
  (-> components components->computations-maps computations-maps->phases))


(defn conf->system [{:keys [inputs components]}]
  (let [phases (components->phases components)
        start-phase (get phases :start)
        {start-computations :computations
         start-computations-names :computation-names} start-phase
        dependency-graph (g/make-graph start-computations)
        order (bb/computations-order dependency-graph start-computations-names)]
    {:state inputs
     :phases phases
     :order order}))


;; -----------------------------------------------------------------------------
;; Api configs
;; -----------------------------------------------------------------------------
(defn compute-on-deps [{:keys [computation deps]}]
  (computation deps))


(defn compute-on-current-val [{:keys [computation current-value]}]
  (computation current-value))


(def common-impl
  {:gather-deps select-keys
   :compute-on-deps compute-on-deps
   :compute-on-current-val compute-on-current-val})


(def impl
  (merge
    common-impl
    {:execute-computations-on-deps (bb/c bb/make-execute-computations
                                         :gather-deps
                                         {:compute-on-deps :compute})

     :execute-computations-on-current-val (bb/c bb/make-execute-computations
                                                :gather-deps
                                                {:compute-on-current-val :compute})}))


(def impl-async
  (merge
    common-impl
    {:combine-map
     (bb/c bb/make-combine-map :combine :then)

     :combine-mixed-map
     (bb/c bb/make-combine-mixed-map :combine-map :promise? :then)

     :gather-deps-async
     (bb/c bb/make-gather-deps-async :gather-deps :combine-mixed-map)

     :compute-on-deps-async (bb/c bb/make-compute-async
                                  :promise? :then
                                  {:compute-on-deps :compute})

     :compute-on-current-val-async (bb/c bb/make-compute-async
                                         :promise? :then
                                         {:compute-on-current-val :compute})

     :execute-computations-on-deps (bb/c bb/make-execute-computations
                                         {:gather-deps-async :gather-deps
                                          :compute-on-deps-async :compute})

     :execute-computations-on-current-val (bb/c bb/make-execute-computations
                                                {:gather-deps-async :gather-deps
                                                 :compute-on-current-val-async :compute})}))

