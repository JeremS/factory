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


(defn conf->system
  "Create a system from a `config` map, if a `component-names` sequence is given, only these
  components (and their dependencies) will be started, stopped...

  `config` must be a map of the following keys:
  - `:inputs`: a map that presumably contains configuration values for components,
    each key being able to be used as a dependency in components compuations.
  - `:components`: a map of components.

  Components are maps of operation names (like `:start` or `:stop`) to computations."
  ([config]
   (conf->system config #{}))
  ([config component-names]
   (let [{:keys [inputs components] :or {inputs {}}} config
         phases (components->phases components)
         start-phase (get phases :start)
         {start-computations :computations
          start-computations-names :computation-names} start-phase
         dependency-graph (g/make-graph start-computations)
         active-components (if (empty? component-names)
                             start-computations-names
                             (->> component-names
                                  set
                                  (g/reachable-from-nodes (g/predecessors dependency-graph))
                                  (filter start-computations-names)
                                  set))
         order (cc/computations-order dependency-graph active-components)]
     {:state inputs
      :phases phases
      :order order})))


;; -----------------------------------------------------------------------------
;; Api building fns
;; -----------------------------------------------------------------------------
(defn compute-on-deps [{:keys [computation deps current-value]}]
  (-> deps
      (vary-meta assoc ::current-value current-value)
      computation))


(defn current-value
  "Get the current value of a component using its dependencies."
  [deps]
  (-> deps meta ::current-value))


(defn compute-on-current-val [{:keys [computation current-value]}]
  (computation current-value))


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
                                                {:compute-on-current-val :compute})}))


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

