(ns fr.jeremyschoffen.factory.core-test
  (:require
    [lazytest.core :as lt :refer [defdescribe it expect expect-it]]
    [fr.jeremyschoffen.factory.core :as c]))


(defn apply-coef [{:keys [v c]}]
  (+ v (* v c)))


(defn sub-total [{:keys [price quantity]}]
  (* price quantity))


(defn sum-vals [m]
  (->> m
       vals
       (apply +)))


(def inputs
  {:price1 10
   :quantity1 6

   :price2 20
   :quantity2 2

   :taxe-coef 0.2})



(def example1
  {:total1 (c/bb sub-total {:price1 :price
                            :quantity1 :quantity})

   :total2  (c/bb sub-total {:price2 :price
                             :quantity2 :quantity})

   :total-bt (c/bb sum-vals :total1 :total2)

   :total (c/bb apply-coef {:total-bt :v
                            :taxe-coef :c})})

(def expected-res
  {:total1 60
   :total2 40
   :total-bt 100
   :total 120.0})


(defdescribe example1-t
  (expect-it "runs the simple example"
    (= (c/run example1 inputs) expected-res)))


(def error-msg "Error in sum-vals")


(defn faulty-sum-vals [& _]
  (throw (ex-info error-msg {})))


(def example-error
  (assoc example1 :total-bt (c/bb faulty-sum-vals :total1 :total2)))



(def error-result
  (try
    (c/run example-error inputs)
    (catch Exception e
      e)))


(def expected-error-res
  {:total1 60
   :total2 40})


(defdescribe error-test
  (it "Handles errors"
    (expect (= error-msg (-> error-result ex-cause ex-message)))
    (expect (= expected-error-res (-> error-result ex-data :current-state)))))

