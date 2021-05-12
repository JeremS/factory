(ns fr.jeremyschoffen.factory.v1.systems.protocols)


(defprotocol Dependent
  :extend-via-metadata true
  (dependent? [this])
  (dependencies [this]))


(defprotocol Computation
  :extend-via-metadata true
  (computation? [this])
  (compute [this deps]))


(extend-type #?(:clj Object :cljs default)
  Dependent
  (dependent? [this] false)
  (dependencies [this] #{})

  Computation
  (computation? [this] false)
  (compute [this deps] nil))
