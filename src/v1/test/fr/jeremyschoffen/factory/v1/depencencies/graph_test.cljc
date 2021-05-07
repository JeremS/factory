(ns fr.jeremyschoffen.factory.v1.depencencies.graph-test
  (:require
    #?(:clj  [clojure.test :refer (deftest testing is)]
       :cljs [cljs.test :refer-macros (deftest testing is)])
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [loom.graph :as loom]))


(def example-graph
  (loom/digraph
    [:a :x]
    [:b :x]
    [:c :y]
    [:x :y]))


(def cyclical-graph
  (loom/add-edges example-graph [:y :a]))


(def n->order
  (->> example-graph
    g/topsort
    (into {}
          (map-indexed (fn [i v] [v i])))))


(defn done-before? [x y]
  (< (n->order x)
     (n->order y)))


(deftest topsort
  (testing "Order validity"
    (is (done-before? :a :x))
    (is (done-before? :b :x))
    (is (done-before? :c :y))
    (is (done-before? :x :y)))

  (testing "Cycles"
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo
                    :cljs cljs.core.ExceptionInfo)
                 (g/topsort cyclical-graph)))))


(def s (loom/successors example-graph))
(def p (loom/predecessors example-graph))


(deftest reach
  (testing "from nodes"
    (is (= (g/reachable-from-nodes s [:a :b]) #{:y :b :x :a}))
    (is (= (g/reachable-from-nodes p [:a :b]) #{:b :a}))
    (is (= (g/reachable-from-nodes s [:c :b]) #{:y :c :b :x}))
    (is (= (g/reachable-from-nodes p [:c :b]) #{:c :b}))))

