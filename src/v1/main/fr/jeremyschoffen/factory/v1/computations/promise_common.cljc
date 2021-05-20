(ns fr.jeremyschoffen.factory.v1.computations.promise-common
  (:require
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]))


(def api-build-conf
  {:combine-map
   (bb/c bb/make-combine-map :combine :then)

   :combine-mixed-map
   (bb/c bb/make-combine-mixed-map :combine-map :promise? :then)

   :gather-deps-async
   (bb/c bb/make-gather-deps-async :gather-deps :combine-mixed-map)

   :compute-async
   (bb/c bb/make-compute-async :compute :promise? :then)

   :execute-computations
   (bb/c bb/make-execute-computations {:gather-deps-async :gather-deps :compute-async :compute})

   :run
   (bb/c bb/make-run :execute-computations :split-config)})
