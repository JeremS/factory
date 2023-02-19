(ns ^{:author "Jeremy Schoffen"
      :doc "
      "}
  fr.jeremyschoffen.factory.v1.dependencies.protocols)


(defprotocol Dependent
  "Protocol allowing implementers to define dependency relationships."
  :extend-via-metadata true
  (dependencies [this] "Return a set of dependencies (presumably names)."))


(extend-type #?(:clj Object :cljs default)
  Dependent
  (dependencies [this] #{}))

