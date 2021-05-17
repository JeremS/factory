(ns fr.jeremyschoffen.factory.v1.computations.building-blocks.manifold
  (:require
    #?(:clj [manifold.deferred :as manifold]
       :cljs [manifold-cljs.deferred :as manifold])))

(def impl
  {:promise? manifold/deferred?
   :combine (fn [promises]
              (apply manifold/zip promises))
   :then manifold/chain})
