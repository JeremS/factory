(ns fr.jeremyschoffen.factory.async.promesa-test
  (:require
    [lazytest.core :as lt :refer [defdescribe it expect expect-it]]
    [promesa.core :as promesa]
    [fr.jeremyschoffen.factory.async.promesa :as p]
    [fr.jeremyschoffen.factory.core :as c]
    [fr.jeremyschoffen.factory.core-test :as ct]))



(defdescribe example1-t
  (expect-it "runs the simple example"
    (= (-> (p/run ct/example1 ct/inputs)
         (p/combine-mixed-map)
         (deref 0 ::error))
       ct/expected-res)))


(defn apply-coef-async [m]
  (promesa/future (ct/apply-coef m)))


(defn ->inputs-2 []
  (assoc ct/inputs :price1 (promesa/deferred)))


(def example2
  (assoc ct/example1
         :total (c/bb apply-coef-async {:total-bt :v
                                        :taxe-coef :c})))

(defdescribe example2-t
  (let [inputs-2 (->inputs-2)
        res-2 (p/combine-mixed-map (p/run example2 inputs-2))]
    (expect-it "returns a promise"
      (promesa/promise? res-2))
    (expect-it "is not realized"
      (promesa/pending? res-2))


    (it "Realizes at the end"
      (promesa/resolve! (:price1 inputs-2) 10)

      (expect (= (deref res-2 100 :promise-not-realized-yet)
                 ct/expected-res)))))
