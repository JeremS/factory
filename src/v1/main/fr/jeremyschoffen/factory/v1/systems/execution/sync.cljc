(ns fr.jeremyschoffen.factory.v1.systems.execution.sync
  (:require
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]))


(def api-config
  {:execute-computation bb/make-execute-computation
   :execute-computations bb/make-execute-computations})
