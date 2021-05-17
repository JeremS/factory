(ns fr.jeremyschoffen.factory.v1.computations.promise-common
  (:require
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]))


(def api-build-conf
  {:combine-map
   (r/c bb/make-combine-map :combine :then)

   :combine-mixed-map
   (r/c bb/make-combine-mixed-map :combine-map :promise? :then)

   :gather-deps-async
   (r/c bb/make-gather-deps-async :gather-deps :combine-mixed-map)

   :compute-async
   (r/c bb/make-compute-async :compute :promise? :then)

   :execute-computation
   (r/c bb/make-execute-computation {:gather-deps-async :gather-deps
                                     :compute-async :compute})

   :execute-computations
   (r/c bb/make-execute-computations :execute-computation)

   :run
   (r/c bb/make-run :execute-computations :split-config)})
