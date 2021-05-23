(ns fr.jeremyschoffen.factory.v1.computations.promise-common
  (:require
    [fr.jeremyschoffen.factory.v1.computations.common :as common]))


(def api-build-conf
  {:combine-map
   (common/c common/make-combine-map :combine :then)

   :combine-mixed-map
   (common/c common/make-combine-mixed-map :combine-map :promise? :then)

   :gather-deps-async
   (common/c common/make-gather-deps-async :gather-deps :combine-mixed-map)

   :compute-async
   (common/c common/make-compute-async :compute :promise? :then)

   :execute-computations
   (common/c common/make-execute-computations {:gather-deps-async :gather-deps :compute-async :compute})

   :run
   (common/c common/make-run :execute-computations :split-config)})
