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

   :total1 (-> sub-total
             (c/wrap-rename-keys {:price1 :price
                                  :quantity1 :quantity})
             (c/c [:price1 :quantity1]))

   :total2 (-> sub-total 
             (c/wrap-rename-keys {:price2 :price
                                  :quantity2 :quantity})
             (c/c [:price2 :quantity2]))

   :total-bt (c/c sum-vals [:total1 :total2])

   :total (-> apply-coef
            (c/wrap-rename-keys {:total-bt :v
                                 :taxe-coef :c})
            (c/c [:total-bt :taxe-coef]))})


(deftest example1-t
  (is (= (c/run example1)
         {:price1 10
          :quantity1 6

          :price2 20
          :quantity2 2

          :taxe-coef 0.2
          :total1 60
          :total2 40
          :total-bt 100
          :total 120.0})))

