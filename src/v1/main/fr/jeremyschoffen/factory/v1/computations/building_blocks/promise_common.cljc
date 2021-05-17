(ns fr.jeremyschoffen.factory.v1.computations.building-blocks.promise-common
  (:require
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


(defn make-combine-map [{:keys [combine then]}]
  (fn combine-map [m]
    (let [[names promises] (m/find m
                             (m/map-of !k !v)
                             [!k !v])]
      (-> (combine promises)
          (then (fn [ps]
                  (zipmap names ps)))))))


(defn make-gather-deps-async [{:keys [gather-deps promise? combine-map then]}]
  (fn gather-deps-async [state deps-names]
    (let [{realized-deps false
           deferred-deps true} (-> state
                                   (gather-deps deps-names)
                                   (u/split-map promise?))]
      (if (empty? deferred-deps)
        realized-deps
        (-> deferred-deps
            combine-map
            (then (fn [newly-realized-deps]
                    (merge realized-deps newly-realized-deps))))))))


(defn make-compute-async [{:keys [compute promise? then]}]
  (fn compute-async [computation current-val deps]
    (if (promise? deps)
      (compute computation current-val deps)
      (-> deps
          (then (fn [realized-deps]
                  (compute computation current-val realized-deps)))))))

