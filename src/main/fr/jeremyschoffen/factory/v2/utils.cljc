(ns ^{:author "Jeremy Schoffen"
      :doc "
Utility functions.
      "}
  fr.jeremyschoffen.factory.v2.utils
  (:require
    [hyperfiddle.rcf :refer [tests]]
    [loom.derived :as gd]
    [loom.graph :as g]))


;; -----------------------------------------------------------------------------
;; Map utils
;; -----------------------------------------------------------------------------
(defn un-zipmap [m]
  (let [ks (volatile! (transient []))
        vs (volatile! (transient []))]
    (doseq [[k v] m]
      (vswap! ks conj! k)
      (vswap! vs conj! v))
    [(-> ks deref persistent!)
     (-> vs deref persistent!)]))

(tests
  (def unzip-ex {:a 1 :b 2})

  (->> unzip-ex
       un-zipmap
       (apply zipmap)) := unzip-ex)


(defn split-seq-on
  "Split `coll` using `v`"
  ([v]
   (comp (partition-by #(= % v))
         (remove #(= % [v]))))
  ([v coll]
   (into (empty coll) (split-seq-on v) coll)))

(tests
  (split-seq-on :b [:a :a :b :a :b :c]) := [[:a :a] [:a] [:c]])


(defn split-map-kv
  "Return a map of maps whose keys are determinded using the function `f` on
  map entries of `m` such as:
  (fn f [[k v]] value-to-group-kvs)

  The values of the result are map whose map entries have the same values by `f` "
  [m f]
  (-> m
      (->> (group-by f))
      (update-vals #(into {} %))))


(defn split-map-by-val
  "Split a map by appling a discrimination function only to its values."
  [m f]
  (split-map-kv m (fn [[_ v]]
                    (f v))))

(tests
  (defn split-even-odd [v]
    (if (even? v) :even :odd))


  (split-map-by-val {:a 1 :b 2 :c 3} split-even-odd)
  := {:odd {:a 1, :c 3}, :even {:b 2}})


(defn split-map-by-key
  "Split a map by appling a discrimination function only to its keys."
  [m f]
  (split-map-kv m (fn [[k _]]
                    (f k))))

(tests
  (split-map-by-key {1 :a 2 :b 3 :c} split-even-odd)
  := {:odd {1 :a, 3 :c}, :even {2 :b}})


(defn reduce-transient
  "Like reduce, makes the accumulator transient then persists the result."
  [f val coll]
  (persistent!
    (reduce f
            (transient val)
            coll)))


(defn select-keys! [m key-seq]
  (reduce-transient
    (fn [acc key]
      (conj! acc (find m key)))
    {}
    key-seq))

(tests
  (select-keys! {:a 1 :b 2 :c 3} [:a :c]) := {:a 1, :c 3})



;; -----------------------------------------------------------------------------
;; Graph utils
;; -----------------------------------------------------------------------------
(defn all-predecessors
  "Return a set of all transitive predecessors of a `node` including the `node`
  passed in parameter"
  [graph node]
  (-> graph
      g/transpose
      (gd/subgraph-reachable-from node)
      g/nodes
      set))


(defn transitive-deps
  "Return a set of all transitive predecessors of a set of `nodes`. The
  `nodes` passed in parameter are included in the resulting set"
  [graph nodes]
  (let [nodes (set nodes)]
    (loop [seen #{}
           [node & rest-nodes] nodes]
      (cond
        (not node) seen

        (contains? seen node)
        (recur seen rest-nodes)

        :else
        (recur (into seen (all-predecessors graph node))
               rest-nodes)))))


(tests
  (def ex-graph
    (g/digraph
      [:a :b]
      [:a :c]
      [:b :d]
      [:c :d]
      [:d :e]
      [:a :b']
      [:b' :c']
      [:c' :d']))

  (all-predecessors ex-graph :c) := #{:c :a}
  (all-predecessors ex-graph :e) := #{:a :b :c :d :e}
  (transitive-deps ex-graph #{:c :d'}) := #{:c' :b' :c :d' :a})


