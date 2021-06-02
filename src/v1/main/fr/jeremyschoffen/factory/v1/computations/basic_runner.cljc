(ns ^{:author "Jeremy Schoffen"
      :doc "
Basic implementation of a `computations-config` runner using the building blocks from [[fr.jeremyschoffen.factory.v1.common]].
      "}
  fr.jeremyschoffen.factory.v1.computations.basic-runner
  (:require
    [fr.jeremyschoffen.factory.v1.computations.common :as common]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


(def ^{:arglists '([f & deps])} c
  "Alias for [[fr.jeremyschoffen.factory.v1.common/c]]."
  common/c)


(def ^{:arglists '([f & deps])} values
  "Alias for [[fr.jeremyschoffen.factory.v1.common/values]]."
  common/values)


(defn compute
  "The compute function used in this implementation."
  [{:keys [computation deps]}]
  (computation deps))


(def ^{:arglists '([initial-state computations order])} execute-computations
  "Execute the `computations` map in `order` starting with an `initial-state` as inputs."
  (common/make-execute-computations
    {:gather-deps select-keys
     :compute compute}))


(defn split-config
  "Split a computations config in 2 maps, a computations map and an inputs map. Return these in a map of 2 keys: `:computations` and `inputs`."
  [m]
  (u/split-map m (fn [x]
                   (if (common/computation? x)
                     :computations
                     :inputs))))


(def ^{:arglists '([computations-config])} run
  "Function that will execute a `computations-config`.
  This function is built using [[fr.jeremyschoffen.factory.v1.common/make-run]]."
  (common/make-run
    {:execute-computations execute-computations
     :split-config split-config}))


(comment
  (def config
    {:a 1
     :b 3
     ::c -5

     :d (c (comp (partial apply +) vals)
           [:a :b])
     :e (c (comp (partial apply +) vals)
           :d
           {::c :c})})

  (run config))

