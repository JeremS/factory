(ns fr.jeremyschoffen.factory.v1.computations.manifold-runner-test
  (:require
    #?(:clj [clojure.test :refer (deftest is)]
       :cljs [cljs.test :refer-macros (deftest is async)])
    #?(:clj [manifold.deferred :as manifold]
       :cljs [manifold-cljs.deferred :as manifold])
    [fr.jeremyschoffen.factory.v1.computations.manifold-runner :as mr]))


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

   :total1 (mr/c sub-total {:price1 :price
                            :quantity1 :quantity})

   :total2  (mr/c sub-total {:price2 :price
                             :quantity2 :quantity})

   :total-bt (mr/c sum-vals :total1 :total2)

   :total (mr/c apply-coef {:total-bt :v
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
  (is (= (mr/run example1) expected-res)))


(defn apply-coef-async [m]
  (manifold/future (apply-coef m)))

(def example2
  (merge
    example1
    {:price1 (manifold/deferred)
     :total (mr/c apply-coef-async {:total-bt :v
                                    :taxe-coef :c})}))

(def res-2 (-> example2
             mr/run
             mr/combine-mixed-map))


(deftest example2-t
  (is (manifold/deferred? res-2))
  (is (not (manifold/realized? res-2)))

  (manifold/success! (:price1 example2) 10)

  #?(:clj
     (is (= (deref res-2 10 :promise-not-realized-yet)
            expected-res))
     :cljs
     (async done
       (manifold/chain res-2
                      (fn [realized-res]
                        (is (= realized-res expected-res))
                        (done))))))

