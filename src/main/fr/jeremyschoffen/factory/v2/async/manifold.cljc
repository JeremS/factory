(ns fr.jeremyschoffen.factory.v2.async.manifold
  (:require
    #?(:clj [manifold.deferred :as manifold]
       :cljs [manifold-cljs.deferred :as manifold])
    [hyperfiddle.rcf :refer [tests]]
    [fr.jeremyschoffen.factory.v2.core :as c]
    [fr.jeremyschoffen.factory.v2.async :as async]))
 

(def promesa-impl
  {:promise? manifold/deferred?
   :combine (fn [promises]
              (apply manifold/zip promises))
   :then manifold/chain
   :make-resolved manifold/success-deferred})


(def api (async/make-api promesa-impl))

(def run (:run api))

(def factory->fn (:factory->fn api))
(def factory->bb (:factory->bb api))

(tests
  (update-vals (run c/ex-factory c/inputs) deref)
  := c/expected-res)


(tests
  (def inputs {:a (manifold/deferred)
               :b (manifold/deferred)
               :c 3})

  (def factory {:d {:deps #{:a :b}
                    :fn (fn [{:keys [a b]}]
                          (+ a b))}

                :e {:deps #{:a :c}
                    :fn (fn [{:keys [a c]}]
                          (+ a c))}})

  (def res (run factory inputs))

  "Nothing resolved yet"
  (-> res :d manifold/realized?) := false
  (-> res :e manifold/realized?) := false

  "Resolving :a"
  (-> inputs :a (manifold/success! 1))

  (-> res :d manifold/realized?) := false
  (-> res :e manifold/realized?) := true
  (-> res :e deref) := 4

  "Resolving :b"
  (-> inputs :b (manifold/success! 2))
  (-> res :d manifold/realized?) := true
  (-> res :e manifold/realized?) := true
  (-> res :d deref) := 3
  ,)

(comment
  (user/reload!))

