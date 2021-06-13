




# Computations

This library provide a way to structure computations similarly to what the
[graph](https://github.com/plumatic/plumbing) provides. Lets take the same example as the one [graph](https://github.com/plumatic/plumbing) uses in
its readme to illustrate the use of computations.

We start by declaring the different functions the overall structured computations will use:


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


We then express the computation with the following:


```clojure

(require '[fr.jeremyschoffen.factory.v1.computations.basic-runner :as r :refer (c values)])

```
```clojure

(def stats-config
  {:xs [1 2 3 6] ; inputs are part of the config
   :n (c n :xs)
   :m (c mean :xs :n)
   :m2 (c mean-square :xs :n)
   :v (c variance :m :m2)})

```


We can then execute this config with:

```clojure

  (r/run stats-config)

```
;=>
```clojure
{:xs [1 2 3 6], :n 4, :m2 25/2, :m 3, :v 7/2}
```
