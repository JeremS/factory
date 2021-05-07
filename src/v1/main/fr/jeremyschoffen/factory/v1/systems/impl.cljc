(ns fr.jeremyschoffen.factory.v1.systems.impl
  (:require
    [clojure.set :as s]
    [fr.jeremyschoffen.factory.v1.systems.protocols :as p]))


(extend-type #?(:clj Object :cljs default)
  p/Dependent
  (dependent? [this] false)
  (dependencies [this] #{})

  p/Computation
  (computation? [this] false)
  (compute [this] nil))


(def simple-impl
  {`p/dependent? (constantly true)
   `p/dependencies (fn [this]
                     (:deps this))
   `p/computation? (constantly true)
   `p/compute (fn [this deps]
                ((:f this) deps))})


(defn c
  "Define a computation from a map.

  key args:
  - `:deps`: names of the dependencies
  - `f`: the computation, a function 'deps -> result'. `deps` is a map of dependencies.
  "
  [m]
  (-> m
    (update :deps set)
    (vary-meta merge simple-impl)))


(comment
  (def ex1 (c {:deps #{:a :b}}))
  (p/dependencies ex1))


(defn wrap-compute-fn-with-aliases [f aliases]
  (fn [this deps]
    (f this (s/rename-keys deps aliases))))


(defn wrap-computation-with-aliases [computation-map aliases]
  (vary-meta computation-map update `p/compute wrap-compute-fn-with-aliases aliases))


(comment
  (def ex
    (c {:deps [:total :taxe-coef]
        :f (fn [{:keys [v c]}]
             (+ v (* v c)))}))

  (p/compute ex {:v 100 :c 0.2})

  (p/compute
    (wrap-computation-with-aliases ex {:total :v :taxe-coef :c})
    {:total 100 :taxe-coef 0.2}))

