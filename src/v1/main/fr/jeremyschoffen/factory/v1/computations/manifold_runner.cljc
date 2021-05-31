(ns ^{:author "Jeremy Schoffen"
      :doc "
Promise based async counterpart of [[fr.jeremyschoffen.factory.v1.computations.basic-runner]] using the manifold library.
      "}
  fr.jeremyschoffen.factory.v1.computations.manifold-runner
  (:require
    #?(:clj [manifold.deferred :as manifold]
       :cljs [manifold-cljs.deferred :as manifold])
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.computations.common :as common]
    [fr.jeremyschoffen.factory.v1.computations.promise-common :as pc]))



(def ^{:arglists '([f & deps])} c
  "Alias for [[fr.jeremyschoffen.factory.v1.common/c]]."
  common/c)


(def ^{:arglists '([f & deps])} values
  "Alias for [[fr.jeremyschoffen.factory.v1.common/values]]."
  common/values)


(def ^:private impl
  {:gather-deps select-keys
   :compute r/compute
   :split-config r/split-config

   :promise? manifold/deferred?
   :combine (fn [promises]
              (apply manifold/zip promises))
   :then manifold/chain
   :make-resolved manifold/success-deferred})


(def ^:private internal-api (r/run (merge impl pc/api-build-conf)))


(def ^{:arglists '([computations-config])} run
  "Function that will execute a `computations-config` allowing for async computations
  using the manifold library. Return a manifold promise.

  This function is built using [[fr.jeremyschoffen.factory.v1.common/make-run]]."
  (:run internal-api))




(comment
  (do
    (defn add [m]
      (println "---------------------------------------")
      (println "getting deps" m)
      (->> m
           vals
           (apply +)))

    (def config
      {:a 1
       :b 3
       :c -5

       :d (c add :a :b)
       :e (c add :c :d)})

    (def config2
      {:a (manifold/deferred)
       :b 3
       :c -5

       :d (c add :a :b)
       :e (c add :c :d)}))

  (run config)
  (def res (run config2))
  (deref res 1 :blocked)
  (manifold/success! (:a config2) 1)
  (deref res 1 :blocked))
