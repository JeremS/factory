(ns fr.jeremyschoffen.factory.v1.systems.protocols)


(defprotocol Dependent
  :extend-via-metadata true
  (dependent? [this])
  (dependencies [this]))


(defprotocol Computation
  :extend-via-metadata true
  (computation? [this])
  (compute [_ deps]))


