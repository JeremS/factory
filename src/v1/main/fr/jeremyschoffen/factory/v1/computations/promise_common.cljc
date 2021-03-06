(ns ^{:author "Jeremy Schoffen"
      :doc "
Common implementation of the async promise based counterpart of [[fr.jeremyschoffen.factory.v1.computations.basic-runner]]
      "}
  fr.jeremyschoffen.factory.v1.computations.promise-common
  (:require
    [fr.jeremyschoffen.factory.v1.computations.common :as common]))


(def api-build-conf
  "Specification of a computations configutation using [[fr.jeremyschoffen.factory.v1.computations.basic-runner]]
  describing how to build an async counter part to the basic runner."
  {:combine-map
   (common/c common/make-combine-map :combine :then)

   :combine-mixed-map
   (common/c common/make-combine-mixed-map :combine-map :promise? :then :make-resolved)

   :gather-deps-async
   (common/c common/make-gather-deps-async :gather-deps :combine-mixed-map)

   :compute-async
   (common/c common/make-compute-async :compute :then)

   :execute-computations
   (common/c common/make-execute-computations {:gather-deps-async :gather-deps :compute-async :compute})

   :execute-computations-async
   (common/c common/make-execute-computations-async :execute-computations :combine-mixed-map)

   :run
   (common/c common/make-run
             :split-config
             {:execute-computations-async :execute-computations})})
