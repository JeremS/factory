(ns fr.jeremyschoffen.factory.v1.computations.promesa-runner
  (:require
    [promesa.core :as promesa]
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.computations.common :as common]
    [fr.jeremyschoffen.factory.v1.computations.promise-common :as pc]))



(def c common/c)


(def values common/values)


(def ^:private impl
  {:gather-deps select-keys
   :compute r/compute
   :split-config r/split-config

   :promise? promesa/deferred?
   :combine promesa/all
   :then promesa/then
   :make-resolved promesa/resolved})


(def ^:private internal-api (r/run (merge impl pc/api-build-conf)))


(def run (:run internal-api))




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
      {:a (promesa/deferred)
       :b 3
       :c -5

       :d (c add :a :b)
       :e (c add :c :d)}))

  (def res (run config2))
  (deref res 1 :blocked)
  (promesa/resolve! (:a config2) 1)
  (deref res 1 :blocked))
