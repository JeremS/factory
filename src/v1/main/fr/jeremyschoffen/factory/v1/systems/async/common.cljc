(ns fr.jeremyschoffen.factory.v1.systems.async.common
  (:require
    [clojure.set :as s]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.systems.base :as b]
    [fr.jeremyschoffen.factory.v1.systems.protocols :as p]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


(defn make-combine-map [{:keys [combine then]}]
  (fn combine-map [m]
    (let [[names promises] (m/find m
                             (m/map-of !k !v)
                             [!k !v])]
      (-> (combine promises)
          (then (fn [ps]
                  (zipmap names ps)))))))


(defn make-gather-deps [{:keys [promise? combine-map then]}]
  (fn gather-deps [state deps-names computation-name]
    (let [{realized-state false
           deferred-state true} (-> state
                                    (select-keys deps-names)
                                    (u/split-map promise?))
          realized-state (b/add-current-val realized-state
                                            (get state computation-name))]
      (if (empty? deferred-state)
        realized-state
        (-> deferred-state
            combine-map
            (then (fn [newly-realized-state]
                    (merge realized-state newly-realized-state))))))))


(defn make-execute-computation [{:keys [gather-deps promise? then]}]
  (fn execute-computation [state computations-map computation-name]
    (let [computation (get computations-map computation-name)
          deps-names (p/dependencies computation)
          deps-map (gather-deps state deps-names computation-name)]
      (if (promise? deps-map)
        (then deps-map
              (fn [dm]
                (p/compute computation dm)))
        (p/compute computation deps-map)))))


(defn make-execute-computations [{:keys [execute-computation]}]
  (fn execute-computations [state computations-map computation-names]
    (persistent!
      (reduce
        (fn [acc computation-name]
          (let [r (execute-computation acc computations-map computation-name)]
            (assoc! acc computation-name r)))
        (transient state)
        computation-names))))


(def api-system
  {:combine-map (b/c {:deps [:combine :then]
                      :f make-combine-map})

   :gather-deps (b/c {:deps [:promise? :combine-map :then]
                      :f make-gather-deps})

   :execute-computation (b/c {:deps [:gather-deps :promise? :then]
                              :f make-execute-computation})

   :execute-computations (b/c {:deps [:execute-computation]
                               :f make-execute-computations})})


(comment
  (-> api-system
      b/system
      ::b/input-names))
