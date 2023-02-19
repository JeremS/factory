(ns fr.jeremyschoffen.factory.v1.computations.basic-runner-test
  (:require
    #?(:clj [clojure.test :refer (deftest is)]
       :cljs [cljs.test :refer-macros (deftest is)])
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as c]))


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

   :total1 (c/c sub-total {:price1 :price
                           :quantity1 :quantity})

   :total2  (c/c sub-total {:price2 :price
                            :quantity2 :quantity})

   :total-bt (c/c sum-vals :total1 :total2)

   :total (c/c apply-coef {:total-bt :v
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
  (is (= (c/run example1) expected-res)))


(def error-msg "Error in sum-vals")


(defn faulty-sum-vals [& args]
  (throw (ex-info error-msg {})))


(def example-error
  {:price1 10
   :quantity1 6

   :price2 20
   :quantity2 2

   :taxe-coef 0.2

   :total1 (c/c sub-total {:price1 :price
                           :quantity1 :quantity})

   :total2  (c/c sub-total {:price2 :price
                            :quantity2 :quantity})

   :total-bt (c/c faulty-sum-vals :total1 :total2)

   :total (c/c apply-coef {:total-bt :v
                           :taxe-coef :c})})


(def expected-error-res
  {:price1 10
   :quantity1 6

   :price2 20
   :quantity2 2

   :taxe-coef 0.2
   :total1 60
   :total2 40})


(def error-result
  (try
    (c/run example-error)
    (catch #?@(:clj [Exception e] :cljs [:default e])
      e)))


(deftest error-test
  (is (= error-msg (-> error-result ex-cause ex-message)))
  (is (= expected-error-res (-> error-result ex-data :current-state))))













