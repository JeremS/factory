(ns fr.jeremyschoffen.factory.v2.async
  (:require
    [fr.jeremyschoffen.factory.v2.core :as core]
    [fr.jeremyschoffen.factory.v2.utils :as u]))

;; -----------------------------------------------------------------------------
;; Promise based async/parallel building blocks
;; -----------------------------------------------------------------------------
(defn make-combine-map
  "Make a combine-map function that will combine a map of promises into the
  promise of a map.

  Args: a map of 2 keys:
  - `:combine`: a function that combines a seq of promises into the promise of
    a seq.
  - `:then`: a 'then' function in a particular promise implementation."
  [{:keys [combine then]}]
  (fn combine-map [m]
    (let [[names promises] (u/un-zipmap m)]
      (-> (combine promises)
          (then (fn [ps]
                  (zipmap names ps)))))))


(def combine-map-c
  {:doc ""
   :deps #{:combine :then}
   :fn make-combine-map})


(defn make-combine-mixed-map
  "Similar to [[make-combine-map]] except that the function created here accepts a map whose values may not be promises.

  Arg: a map with the keys:
  - `:combine-map`: a 'combine-map' function such as one made using
    [[make-combine-map]]
  - `:promise?`: a function telling wether a value is a promise or not
  - `:then`: a 'then' function in a particular promise implementation
  - `:make-resolved`: a function that makes a resolved promise
  "
  [{:keys [combine-map promise? then make-resolved]}]
  (fn combine-mixed-map [m]
    (let [{realized-part false
           deferred-part true} (u/split-map-by-val m promise?)]
      (if (empty? deferred-part)
        (make-resolved realized-part)
        (-> deferred-part
            combine-map
            (then (fn [newly-realized-deps]
                    (merge realized-part newly-realized-deps))))))))

(def combine-mixed-map-c
  {:doc ""
   :deps #{:combine-map :promise? :then :make-resolved}
   :fn make-combine-mixed-map})


(defn make-compute-async
  "Wrap the compute function such that can accept async deps. The compute
  function now returns a promise."
  [{:keys [compute combine-mixed-map then]}]
  (fn compute-async [{:keys [deps] :as ctxt}]
    (-> deps
        combine-mixed-map
        (then (fn [realized-deps]
                (-> ctxt
                    (assoc :deps realized-deps)
                    compute))))))


(def compute-async-c
  {:doc "Builds an async compute from a sync one."
   :deps #{:compute :combine-mixed-map :then}
   :fn make-compute-async})


(def compute-async-factory
  {:combine-map combine-map-c
   :combine-mixed-map combine-mixed-map-c
   :compute-async compute-async-c})



(def api-factory
  (-> core/api-factory
      (merge compute-async-factory)
      (assoc-in [:execute-bb :renames] {:compute-async :compute})
      (merge (core/inputs->factory core/api-inputs))))


(def make-api (core/factory->fn api-factory))


