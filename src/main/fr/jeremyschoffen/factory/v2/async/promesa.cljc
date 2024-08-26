(ns fr.jeremyschoffen.factory.v2.async.promesa
  (:require
    [hyperfiddle.rcf :refer [tests]]
    [fr.jeremyschoffen.factory.v2.core :as c]
    [fr.jeremyschoffen.factory.v2.async :as async]
    [promesa.core :as promesa]))


(def promesa-impl
  {:promise? promesa/deferred?
   :combine promesa/all
   :then promesa/then
   :make-resolved promesa/resolved})


(def api (async/make-api promesa-impl))

(def run (:run api))

(def factory->fn (:factory->fn api))
(def factory->bb (:factory->bb api))



(tests
  (update-vals (run c/ex-factory c/inputs) deref)
  := c/expected-res)

(tests
  (def inputs {:a (promesa/deferred)
               :b (promesa/deferred)
               :c 3})

  (def factory {:d {:deps #{:a :b}
                    :fn (fn [{:keys [a b]}]
                          (+ a b))}

                :e {:deps #{:a :c}
                    :fn (fn [{:keys [a c]}]
                          (+ a c))}})

  (def res (run factory inputs))

  "Nothing resolved yet"
  (-> res :d promesa/resolved?) := false
  (-> res :e promesa/resolved?) := false

  "Resolving :a"
  (-> inputs :a (promesa/resolve! 1))

  (-> res :d promesa/resolved?) := false
  (-> res :e promesa/resolved?) := true
  (-> res :e deref) := 4

  "Resolving :b"
  (-> inputs :b (promesa/resolve! 2))
  (-> res :d promesa/resolved?) := true
  (-> res :e promesa/resolved?) := true
  (-> res :d deref) := 3
  ,)

(comment
  (user/reload!))
