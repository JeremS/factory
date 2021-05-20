(ns fr.jeremyschoffen.factory.v1.systems.common
 (:require
   [meander.epsilon :as m]
   [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]
   [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]))


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


(defn triples->computation-phases [triples]
  (->> triples
    triple->transient-computations!
    persist!))


(defn components->computation-phases [components]
  (-> components components->triples triples->computation-phases))


(defn expand-computations [computations]
  (let [computation-names (bb/computation-names computations)
        graph (g/make-graph computations)
        inputs (g/starting-points graph)
        order (bb/computations-order graph computation-names)]
    {:computations computations
     :computation-names computation-names
     :dependency-graph graph
     :inputs inputs
     :order order}))


(defn expand-phases [phases]
  (reduce-kv
    (fn [acc k v]
      (assoc acc k (expand-computations v)))
    {}
    phases))


(defn conf->system [{:keys [inputs components] :as conf}]
  (let []
    {:state inputs
     :phases (-> components
               components->computation-phases
               expand-phases)}))


(defn compute-start-like [{:keys [computation deps]}]
  (computation deps))


(defn compute-stop-like [{:keys [computation current-value]}]
  (computation current-value))


(def common-impl
  {:gather-deps select-keys
   :compute-start-like compute-start-like
   :compute-stop-like compute-stop-like})


(def impl
  (merge
    common-impl
    {:execute-computations-start-like (bb/c bb/make-execute-computations
                                            :gather-deps
                                            {:compute-start-like :compute})

     :execute-computations-stop-like (bb/c bb/make-execute-computations
                                           :gather-deps
                                           {:compute-stop-like :compute})}))

(def impl-async
  (merge
    common-impl
    {:combine-map
     (bb/c bb/make-combine-map :combine :then)

     :combine-mixed-map
     (bb/c bb/make-combine-mixed-map :combine-map :promise? :then)

     :gather-deps-async
     (bb/c bb/make-gather-deps-async :gather-deps :combine-mixed-map)

     :compute-start-like-async (bb/c bb/make-compute-async
                                     :promise? :then
                                     {:compute-start-like :compute})

     :compute-stop-like-async (bb/c bb/make-compute-async
                                    :promise? :then
                                    {:compute-stop-like :compute})

     :execute-computations-start-like (bb/c bb/make-execute-computations
                                            {:gather-deps-async :gather-deps
                                             :compute-start-like-async :compute})

     :execute-computations-stop-like (bb/c bb/make-execute-computations
                                           {:gather-deps-async :gather-deps
                                            :compute-stop-like-async :compute})}))
