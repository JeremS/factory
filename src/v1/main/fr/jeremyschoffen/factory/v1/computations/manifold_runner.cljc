(ns fr.jeremyschoffen.factory.v1.computations.manifold-runner
  (:require
    #?(:clj [manifold.deferred :as manifold]
       :cljs [manifold-cljs.deferred :as manifold])
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]
    [fr.jeremyschoffen.factory.v1.computations.promise-common :as pc]))



(def c bb/c)


(def impl
  {:gather-deps select-keys
   :compute r/compute
   :split-config r/split-config
   :promise? manifold/deferred?
   :combine (fn [promises]
              (apply manifold/zip promises))
   :then manifold/chain})


(def api (r/run (merge impl pc/api-build-conf)))


(def combine-mixed-map (:combine-mixed-map api))


(def run (:run api))




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
  (def res (combine-mixed-map (run config2)))
  (deref res 1 :blocked)
  (manifold/success! (:a config2) 1)
  (deref res 1 :blocked))
