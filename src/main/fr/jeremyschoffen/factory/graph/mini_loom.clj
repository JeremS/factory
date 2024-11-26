(ns fr.jeremyschoffen.factory.graph.mini-loom
  (:require
    [hyperfiddle.rcf :refer [tests]]))

;; Graph implementation and graph Algorithms taken from the loom library.
;; https://github.com/aysylu/loom

;; -----------------------------------------------------------------------------
;; Graph implementation
;; -----------------------------------------------------------------------------
;; Adapted subset of the ns loom.graph
(defrecord BasicEditableDigraph [nodeset adj in])


(defn nodes [g]
  (:nodeset g))


(defn has-node? [g node]
  (contains? (:nodeset g) node))


(defn has-edge? [g n1 n2]
  (contains? (get-in g [:adj n2]) n2))


(defn successors*
  [g node]
  (get-in g [:adj node] #{}))


(defn out-edges [g node]
  (for [n2 (successors* g node)]
    [node n2]))


(defn out-degree [g node]
  (count (get-in g [:adj node])))


(defn edges [g]
  (for [n (nodes g)
        e (out-edges g n)]
    e))


(defn predecessors*
  [g node]
  (get-in g [:in node] #{}))


(defn in-degree [g node]
  (count (get-in g [:in node])))


(defn in-edges [g node]
  (for [n2 (predecessors* g node)]
    [n2 node]))


(defn- remove-adj-nodes [m nodes adjacents remove-fn]
  (reduce
   (fn [m n]
     (if (m n)
       (update-in m [n] #(apply remove-fn % nodes))
       m))
   (apply dissoc m nodes)
   adjacents))


(defn add-nodes* [g nodes]
  (update g :nodeset #(apply conj % nodes)))


(defn add-edges* [g edges]
  (reduce
    (fn [g [n1 n2]]
      (-> g
          (update :nodeset conj n1 n2)
          (update-in [:adj n1] (fnil conj #{}) n2)
          (update-in [:in n2] (fnil conj #{}) n1)))
    g edges))


(defn remove-nodes* [g nodes]
  (let [ins (mapcat #(predecessors* g %) nodes)
        outs (mapcat #(successors* g %) nodes)]
    (-> g
        (update :nodeset #(apply disj % nodes))
        (assoc :adj (remove-adj-nodes (:adj g) nodes ins disj))
        (assoc :in (remove-adj-nodes (:in g) nodes outs disj)))))


(defn remove-edges* [g edges]
  (reduce
    (fn [g [n1 n2]]
      (-> g
          (update-in [:adj n1] (disj n2))
          (update-in [:in n2] (disj n1))))
    g edges))


(defn remove-all [g]
  (assoc g :nodeset #{} :adj {} :in {}))


(defn transpose [g]
  (assoc g :adj (:in g) :in (:adj g)))


(defn successors
  ([g]
   #(successors g %))
  ([g node]
   (successors* g node)))


(defn predecessors
  ([g]
   #(predecessors g %))
  ([g node]
   (predecessors* g node)))


(defn add-nodes [g & nodes]
  (add-nodes* g nodes))


(defn add-edges [g & edges]
  (add-edges* g edges))


(defn remove-nodes [g & nodes]
  (remove-nodes* g nodes))


(defn remove-edges [g & edges]
  (remove-edges* g edges))


(defn build-graph [g inits]
  (letfn [(build [g init]
            (if (sequential? init)
              (add-edges g init)
              (add-nodes g init)))]
    (reduce build g inits)))


(defn digraph* [inits]
  (build-graph (BasicEditableDigraph. #{} {} {}) inits))


(defn digraph [& inits]
  (digraph* inits))


;; -----------------------------------------------------------------------------
;; Algorithms
;; -----------------------------------------------------------------------------
;; Taken from the loom.alg and loom.alg-generic namespaces
(defn pre-traverse
  "Traverses a graph depth-first preorder from start, successors being
  a function that returns direct successors for the node. Returns a
  lazy seq of nodes."
  [successors start & {:keys [seen] :or {seen #{}}}]
  (letfn [(step [stack seen]
            (when-let [node (peek stack)]
              (if (contains? seen node)
                (step (pop stack) seen)
                (let [seen (conj seen node)
                      nbrs (remove seen (successors node))]
                  (lazy-seq
                    (cons node
                          (step (into (pop stack) nbrs)
                                seen)))))))]
    (step [start] seen)))

(tests
  (def g (digraph [:a :b] [:a :c] [:b :d] [:c :d]))

  (set (pre-traverse (successors g) :a)) := #{:c :b :d :a}
  (set (pre-traverse (successors g) :c)) := #{:c  :d}
  (set (pre-traverse (predecessors g) :c)) := #{:c :a}
  (set (pre-traverse (predecessors g) :a)) :=  #{:a}
  (set (pre-traverse (predecessors g) :d)) :=  #{:a :b :c :d})


(defn topsort-component
  "Topological sort of a component of a (presumably) directed graph.
  Returns nil if the graph contains any cycles. See loom.alg/topsort
  for a complete topological sort"
  ([successors start]
   (topsort-component successors start #{} #{}))
  ([successors start seen explored]
   (loop [seen seen
          explored explored
          result ()
          stack [start]]
     (if (empty? stack)
       result
       (let [v (peek stack)
             seen (conj seen v)
             us (remove explored (successors v))]
         (if (seq us)
           (when-not (some seen us)
             (recur seen explored result (conj stack (first us))))
           (recur seen (conj explored v) (conj result v) (pop stack))))))))


(defn topsort
  "Topological sort of a directed acyclic graph (DAG). Returns nil if
  g contains any cycles."
  ([g]
   (loop [seen #{}
          result ()
          [n & ns] (seq (nodes g))]
     (if-not n
       result
       (if (seen n)
         (recur seen result ns)
         (when-let [cresult (topsort-component (successors g) n seen seen)]
           (recur (into seen cresult) (concat cresult result) ns))))))
  ([g start]
   (topsort-component (successors g) start)))

(tests
  (first (topsort g)) := :a
  (last (topsort g)) := :d
  (topsort (digraph)) := '()
  (topsort (digraph [:a :b] [:b :a])):= nil)

