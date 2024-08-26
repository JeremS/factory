(ns ^{:author "Jeremy Schoffen"
      :doc "Base protocols for dependencies."}
  fr.jeremyschoffen.factory.v2.dependencies.protocols)


(defprotocol Ided
  :extend-via-metadata true
  (id [this] "Get the Id of this"))


(defprotocol Dependent
  "Protocol allowing implementers to define dependency relationships."
  :extend-via-metadata true
  (dependencies [this] "Return a set of dependencies (presumably names)."))


(extend-type #?(:clj Object :cljs default)
  Ided
  (id [this] nil)

  Dependent
  (dependencies [this] #{}))


