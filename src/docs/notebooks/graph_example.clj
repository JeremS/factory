(ns notebooks.graph-example
  (:require
    [notebooks :as n]
    [scicloj.kindly.v4.kind :as kind]))


^:kindly/hide-code
(kind/md
  (str "This library provides a way to structure computations inspired by what
       the " n/plumatic-graph " library provides."))


;;  There are 2 concepts :
;; - building blocks: a building block is a unit of computation with declared
;; dependencies
;; - factories: a factory is a map of building blocks

^:kindly/hide-code
(kind/md
 (str
  "Lets take the same example as the one " n/plumatic-graph " README uses in
  its readme to illustrate the use of factories."))
 

;; We start by defining some funcions: 

(defn n [{:keys [xs]}]
  (count xs))


(defn mean [{:keys [xs n]}]
  (/ (reduce + xs)
    n))


(defn mean-square [{:keys [xs n]}]
  (/ (transduce (map #(* % %))
                +
                0
                xs)
    n))


(defn variance [{:keys [m m2]}]
  (- m2 (* m m)))



;; Now let make the factory, a map of building blocks:

(def stats-factory
  {:n  {:deps #{:xs}
        :fn n}
   :m  {:deps #{:xs :n}
        :fn mean}
   :m2 {:deps #{:xs :n}
        :fn mean-square}
   :v  {:deps #{:m :m2}
        :fn variance}})



(require '[fr.jeremyschoffen.factory.core :as f :refer [bb]])

;; We can then execute this factory with:

(f/run stats-factory {:xs [1 2 3 6]})


;; There is a short hand to assemble building blocks.
;; We can declare the factory this way:

(def stats-factory-2
  {:n  (bb n :xs)
   :m  (bb mean :xs :n)
   :m2 (bb mean-square :xs :n)
   :v  (bb variance :m :m2)})


;; We can also turn factories into functions:
;; 
(def f2 (f/factory->fn stats-factory-2))



(f2 {:xs [1 2 3 6]})


^:kindly/hide-code
(comment
  (require '[scicloj.clay.v2.api :as clay])
  (clay/make! {:source-path "src/docs/notebooks/graph_example.clj"
               :live-reload true}))
