(ns fr.jeremyschoffen.factory.v1.systems.promesa
  (:require
    [promesa.core :as promesa]
    [fr.jeremyschoffen.factory.v1.systems.async.common :as async-common]
    [fr.jeremyschoffen.factory.v1.systems.base :as b]))


(def base-impl
  {:promise? promesa/deferred?
   :combine promesa/all
   :then promesa/then})



(def derived-impls
  (b/execute-computations
    base-impl
    async-common/api-components
    (::b/total-order async-common/api-system)))


(def promise? (:promise? derived-impls))
(def combine (:combine derived-impls))
(def then (:then derived-impls))
(def combine-map (:combine-map derived-impls))
(def gather-deps (:gather-deps derived-impls))
(def execute-computation (:execute-computation derived-impls))
(def execute-computations (:execute-computations derived-impls))

(comment
  (def example
    {:a (promesa/deferred)
     :b 3
     :c (b/c {:deps [:a :b]
              :f (fn [m]
                   (->> m
                     vals
                     (apply +)))})})
  (def res
    (combine-map
      (execute-computations
        (select-keys example #{:a :b})
        (select-keys example #{:c})
        [:c])))
  (promesa/done? res)
  (promesa/resolve! (:a example) 1)
  (deref res))

