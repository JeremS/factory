(ns fr.jeremyschoffen.factory.v1.systems.common
 (:require
   [meander.epsilon :as m]
   [fr.jeremyschoffen.factory.v1.computations.common :as cc]
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
   :computation-names (cc/computation-names computations)})


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
        order (cc/computations-order dependency-graph start-computations-names)]
    {:state inputs
     :phases phases
     :order order}))


;; -----------------------------------------------------------------------------
;; Api building fns
;; -----------------------------------------------------------------------------
(defn compute-on-deps [{:keys [computation deps]}]
  (computation deps))


(defn compute-on-current-val [{:keys [computation current-value]}]
  (computation current-value))


(defn make-phase-runner [{:keys [execute-computations phase-name keep-state reverse-order]
                          :or {keep-state true reverse-order false}}]
  (fn [{:keys [state phases order] :as system}]
    (let [phase (get phases phase-name)
          {:keys [computations computation-names]} phase
          order (cond-> (filterv computation-names order)
                  reverse-order rseq)
          new-state (execute-computations state computations order)]
       (cond-> system
         keep-state (assoc :state new-state)))))


(defn make-phase-runner-async [{:keys [combine-mixed-map] :as deps}]
  (let [run-phase (make-phase-runner deps)]
    (fn [map-arg]
      (-> map-arg run-phase combine-mixed-map))))


;; -----------------------------------------------------------------------------
;; Api configs
;; -----------------------------------------------------------------------------
(def common-impl
  {:gather-deps select-keys
   :compute-on-deps compute-on-deps
   :compute-on-current-val compute-on-current-val})


(def impl
  (merge
    common-impl
    {:execute-computations-on-deps (cc/c cc/make-execute-computations
                                         :gather-deps
                                         {:compute-on-deps :compute})

     :execute-computations-on-current-val (cc/c cc/make-execute-computations
                                                :gather-deps
                                                {:compute-on-current-val :compute})

     :run-start (cc/c make-phase-runner
                      {:execute-computations-on-deps :execute-computations}
                      (cc/values
                        :phase-name :start
                        :keep-state true
                        :reverse-order false))

     :run-stop (cc/c make-phase-runner
                     {:execute-computations-on-current-val :execute-computations}
                     (cc/values
                       :phase-name :stop
                       :keep-state false
                       :reverse-order true))}))


(def impl-async
  (merge
    common-impl
    {:combine-map
     (cc/c cc/make-combine-map :combine :then)

     :combine-mixed-map
     (cc/c cc/make-combine-mixed-map :combine-map :promise? :then)

     :gather-deps-async
     (cc/c cc/make-gather-deps-async :gather-deps :combine-mixed-map)

     :compute-on-deps-async (cc/c cc/make-compute-async
                                  :promise? :then
                                  {:compute-on-deps :compute})

     :compute-on-current-val-async (cc/c cc/make-compute-async
                                         :promise? :then
                                         {:compute-on-current-val :compute})

     :execute-computations-on-deps (cc/c cc/make-execute-computations
                                         {:gather-deps-async :gather-deps
                                          :compute-on-deps-async :compute})

     :execute-computations-on-current-val (cc/c cc/make-execute-computations
                                                {:gather-deps-async :gather-deps
                                                 :compute-on-current-val-async :compute})}))

