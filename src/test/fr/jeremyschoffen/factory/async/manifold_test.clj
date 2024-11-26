(ns fr.jeremyschoffen.factory.async.manifold-test
  (:require
    [lazytest.core :as lt :refer [defdescribe it expect expect-it]]
    [manifold.deferred :as manifold]
    [fr.jeremyschoffen.factory.async.manifold :as mr]
    [fr.jeremyschoffen.factory.core :as c]
    [fr.jeremyschoffen.factory.core-test :as ct]))



(defdescribe example1-t
  (expect-it "runs the simple example"
    (= (-> (mr/run ct/example1 ct/inputs)
           (mr/combine-mixed-map)
           (deref 0 ::error))
       ct/expected-res)))

(defn apply-coef-async [m]
  (manifold/future (ct/apply-coef m)))


(defn ->inputs-2 []
  (assoc ct/inputs :price1 (manifold/deferred)))


(def example2
  (assoc ct/example1
         :total (c/bb apply-coef-async {:total-bt :v
                                        :taxe-coef :c})))


(defdescribe example2-t
  (let [inputs-2 (->inputs-2)
        res-2 (mr/combine-mixed-map (mr/run example2 inputs-2))]

    (expect-it "returns a deferred"
      (manifold/deferred? res-2))
    (expect-it "is not realized"
      (not (manifold/realized? res-2)))

    (it "Realizes at the end"
      (manifold/success! (:price1 inputs-2) 10)

      (expect (= (deref res-2 100 :promise-not-realized-yet)
                 ct/expected-res)))))

