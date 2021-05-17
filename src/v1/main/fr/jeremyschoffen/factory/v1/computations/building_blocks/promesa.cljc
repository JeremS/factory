(ns fr.jeremyschoffen.factory.v1.computations.building-blocks.promesa
  (:require
    [promesa.core :as promesa]))


(def impl
  {:promise? promesa/deferred?
   :combine promesa/all
   :then promesa/then})
