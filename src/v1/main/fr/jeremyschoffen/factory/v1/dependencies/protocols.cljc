(ns fr.jeremyschoffen.factory.v1.dependencies.protocols)


(defprotocol Dependent
  :extend-via-metadata true
  (dependent? [this])
  (dependencies [this]))


(extend-type #?(:clj Object :cljs default)
  Dependent
  (dependent? [this] false)
  (dependencies [this] #{}))

