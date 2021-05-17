(ns fr.jeremyschoffen.factory.v1.computations.promesa-runner-test
  (:require
    #?(:clj [clojure.test :refer (deftest is)]
       :cljs [cljs.test :refer-macros (deftest is async)])
    [promesa.core :as promesa]
    [fr.jeremyschoffen.factory.v1.computations.promesa-runner :as pr]))


(defn apply-coef [{:keys [v c]}]
  (+ v (* v c)))


(defn sub-total [{:keys [price quantity]}]
  (* price quantity))


(defn sum-vals [m]
  (->> m
       vals
       (apply +)))


(def example1
  {:price1 10
   :quantity1 6

   :price2 20
   :quantity2 2

   :taxe-coef 0.2

   :total1 (pr/c sub-total {:price1 :price
                            :quantity1 :quantity})

   :total2  (pr/c sub-total {:price2 :price
                             :quantity2 :quantity})

   :total-bt (pr/c sum-vals :total1 :total2)

   :total (pr/c apply-coef {:total-bt :v
                            :taxe-coef :c})})


(def expected-res
  {:price1 10
   :quantity1 6

   :price2 20
   :quantity2 2

   :taxe-coef 0.2
   :total1 60
   :total2 40
   :total-bt 100
   :total 120.0})


(deftest example1-t
  (is (= (pr/run example1) expected-res)))


(defn apply-coef-async [m]
  (promesa/future (apply-coef m)))

(def example2
  (merge
    example1
    {:price1 (promesa/deferred)
     :total (pr/c apply-coef-async {:total-bt :v
                                    :taxe-coef :c})}))


(def res-2 (-> example2
             pr/run
             pr/combine-mixed-map))


(deftest example2-t

  (is (promesa/promise? res-2))
  #?(:clj (is (promesa/pending? res-2)))

  (promesa/resolve! (:price1 example2) 10)

  #?(:clj
     (is (= (deref res-2 1 ::error)
            expected-res))
     :cljs
     (async done
       (promesa/then res-2
                     (fn [realized-res]
                       (is (= realized-res expected-res))
                       (done))))))

