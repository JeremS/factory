(ns fr.jeremyschoffen.factory.v1.systems.base-test
  (:require
    #?(:clj [clojure.test :refer (deftest testing is)]
       :cljs [cljs.test :refer (deftest testing is)])
    [fr.jeremyschoffen.factory.v1.systems.base :as b]))



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

   :total1 (b/wrap-computation-with-aliases
             (b/c {:deps [:price1 :quantity1]
                   :f sub-total})
             {:price1 :price
              :quantity1 :quantity})

   :total2 (b/wrap-computation-with-aliases
             (b/c {:deps [:price2 :quantity2]
                   :f sub-total})
             {:price2 :price
              :quantity2 :quantity})

   :total-bt (b/c {:deps [:total1 :total2]
                   :f sum-vals})

   :total (b/wrap-computation-with-aliases
            (b/c {:deps [:total-bt :taxe-coef]
                  :f apply-coef})
            {:total-bt :v
             :taxe-coef :c})})


(def system1
  (b/system example1))


(deftest example1-t
  (let [{::b/keys [initial-state computations-map total-order]} system1
        res (:res (b/execute-computations
                    initial-state
                    computations-map
                    total-order))]
    (println res)
    (is (= res
           {:total1 60
            :total2 40
            :total-bt 100
            :total 120.0}))))

