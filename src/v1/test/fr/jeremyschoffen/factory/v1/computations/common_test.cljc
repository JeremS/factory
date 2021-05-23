(ns fr.jeremyschoffen.factory.v1.computations.common-test
  (:require
      #?(:clj [clojure.test :refer (deftest is)]
         :cljs [cljs.test :refer-macros (deftest is)])
      [fr.jeremyschoffen.factory.v1.computations.building-blocks :as common]))


(def example-deps
  [:a :b {:namespaced/a :a} ^::common/options {:opt1 1} (common/options :opt2 2)])


(def parsed (common/parse-deps example-deps))


(deftest deps-parsing
  (is (= (:deps parsed) #{:namespaced/a :b}))
  (is (= (:alias-map parsed) {:namespaced/a :a}))
  (is (= (:options parsed) {:opt1 1 :opt2 2})))


