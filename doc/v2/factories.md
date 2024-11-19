

# Factories

This library provides a way to structure computations inspired by what
the [graph](https://github.com/plumatic/plumbing) provides.


There are 2 concepts :
- building blocks: a building block is a unit of computation with declared
  dependencies
- factories: a factory is a map of building blocks


Lets take the same example as the one [graph](https://github.com/plumatic/plumbing)
uses in its readme to illustrate the use of factories.

```clojure

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

```


Now let make the factory, a map of building blocks:

```clojure

(def stats-factory
  {:n  {:deps #{:xs}
        :fn n}
   :m  {:deps #{:xs :n}
        :fn mean}
   :m2 {:deps #{:xs :n}
        :fn mean-square}
   :v  {:deps #{:m :m2}
        :fn variance}})

```


```clojure

(require '[fr.jeremyschoffen.factory.v2.core :as f :refer [bb]])

```


We can then execute this factory with:

```clojure

  (f/run stats-factory {:xs [1 2 3 6]})

```
;=>
```clojure
{:n 4, :m2 25/2, :m 3, :v 7/2}
```

Base on the V1 there is a short hand to assemble building blocks.
We can declare the factory this way:

```clojure

(def stats-factory-2
  {:n  (bb n :xs)
   :m  (bb mean :xs :n)
   :m2 (bb mean-square :xs :n)
   :v  (bb variance :m :m2)})

```

We can also turn factories into functions:

```clojure

(def f2 (f/factory->fn stats-factory-2))

```


```clojure

  (f2 {:xs [1 2 3 6]})

```
;=>
```clojure
{:n 4, :m2 25/2, :m 3, :v 7/2}
```


