(ns ^{:author "Jeremy Schoffen"
      :doc "
Common building blocks used to execute DAGs of interdependent computations.

Lexicon:
- 'building block': a unit of computation to build something
- 'factory': a map of build block ids to building blocks
      "}
  fr.jeremyschoffen.factory.v2.common
  (:require
    [hyperfiddle.rcf :refer [tests]]
    [fr.jeremyschoffen.factory.v2.utils :as u]
    [loom.alg :as ga]
    [loom.graph :as g]))

;; TODO: Add error on cycles

;; -----------------------------------------------------------------------------
;; Turning a factory into a graph
;; -----------------------------------------------------------------------------
(defn- bb->edges
  "Takes an `id` and `deps` a seq of edges from deps to id in order to
  construct a dependency graph. "
  [id deps]
  (if (seq deps)
    (mapv (fn [dep] [dep id]) deps)
    [id]))

(tests
  (set (bb->edges :c #{:a :b}))
  := #{[:a :c] [:b :c]})


(defn- factory->edges
  "Return a seq of edges used to make a dependency graph from a factory."
  [factory get-deps]
  (mapcat (fn [[id c]]
            (bb->edges id (get-deps c)))
          factory))

(tests
  "Defining a simple factory"
 
  (def ex-factory
    {:d {:deps #{:a :b}
         :fn (comp (partial apply +) vals)}

     :e {:deps #{:d :c}
         :fn (comp (partial apply +) vals)}

     :f {:deps #{}
         :fn (fn [_] :no-deps)}})


  (set (factory->edges ex-factory :deps))
  := #{[:a :d] [:b :d] [:c :e] [:d :e] :f})


(defn make-factory->graph
  "Returns a function that take a factory and turns it into a
  dependency graph.

  Arg map:
  - `get-deps`: a function that gets the dependencies of a building block

  (get-deps c) ;=> #{:deps1 :deps2}"
  [{:keys [get-deps]}]
  (fn factory->graph [map-config]
    (-> map-config
         (factory->edges get-deps)
         (->> (apply g/digraph)))))


(def ^:private factory->graph
  (make-factory->graph {:get-deps :deps}))


(tests
  (def ex-graph (factory->graph ex-factory))

  (:nodeset ex-graph) := #{:a :b :c :d :e :f}

  (:adj ex-graph) := {:a #{:d}
                      :b #{:d}
                      :c #{:e}
                      :d #{:e}}

  (:in ex-graph) := {:d #{:b :a}
                     :e #{:c :d}})


;; -----------------------------------------------------------------------------
;; System info
;; -----------------------------------------------------------------------------
(defn get-bb-ids
  "Get a set of building block ids from a factory."
  [factory]
  (-> factory keys set))


(tests
  (def ex-names (get-bb-ids ex-factory))

  ex-names
  := #{:d :e :f})


(defn topological-order
  "Get a topological sort from a dependency graph in which non building blocks
  are filtered out. This gives us a valid order in which to run the
  building blocks."
  [dependency-graph bb-ids]
  (->> dependency-graph
    ga/topsort
    (filterv (set bb-ids))))


(tests
  (topological-order ex-graph ex-names) := [:f :d :e])


(defn make-get-input-names
  "Makes a function that get input names given a factory."
  [{:keys [get-deps]}]
  (fn input-names [factory]
    (let [c-names (get-bb-ids factory)]
      (into #{}
            (comp
              (map val)
              (mapcat get-deps)
              (remove c-names))
            factory))))

(def ^:private get-input-names (make-get-input-names {:get-deps :deps}))

(tests
  (get-input-names ex-factory)
  := #{:a :b :c})


;; -----------------------------------------------------------------------------
;; Execution functions
;; -----------------------------------------------------------------------------
(defn make-execute-bb
  "Make a function that will execute one building-block."
  [{:keys [get-deps gather-deps compute]}]
  (fn execute-bb [inputs bb-id bb]
    (let [dep-names (get-deps bb)
          deps (gather-deps inputs dep-names)
          current-val (get inputs bb-id)]
      (compute {:bb-id bb-id
                :bb bb
                :current-value current-val
                :deps deps}))))


(defn basic-compute
  "Basic compute function, just passes the deps to the building block's
  function."
  [{:keys [bb deps]}]
  (let [f (:fn bb)]
    (f deps)))


(def ^:private execute-bb
  (make-execute-bb {:get-deps :deps
                    :gather-deps select-keys
                    :compute basic-compute}))


(defn- try-execute-bb [exec state bb-id bb]
  (try
    (exec state bb-id bb)
    (catch #?@(:clj [Exception e] :cljs [:default e])
      (throw (ex-info (str "Error while running: " bb-id)
                      {:current-state (persistent! state)}
                      e)))))


(defn make-execute-bbs
  "Make a function that will execute a factory.

  Map arg keys:
  - `get-deps`: get the dependencies of a building block
  - `gather-deps`: a function
    (state, dependency names) -> map of (dependency names -> dependency val)
  - `compute`: the function that will perform the execution of a building block

  The `compute` function receives one argument, a map whose keys are:
  - `:bb-id`: the id of the building block
  - `:bb`: the building block
  - `:current-value`: when building systems, the current value of the building
  block, usefull to pass some statefull thing to shut down.
  - `deps`: the map of dependencies
  "
  [{:keys [execute-bb]}]
  (fn execute-bbs [factory inputs order]
    (-> (u/reduce-transient
          (fn execute-one-bb [state bb-id]
            (let [bb (get factory bb-id)
                  res (try-execute-bb execute-bb state bb-id bb)]
              (assoc! state bb-id res)))
          inputs
          order)
        (u/select-keys! order))))

(def ^:private execute-bbs
  (make-execute-bbs {:execute-bb execute-bb}))


(defn compute-order
  "Returns the execution order for a factory. It does so by filtering for the
  building blocks ids in a topological sort of the dependency graph."
  [factory factory->graph]
  (topological-order (factory->graph factory)
                     (get-bb-ids factory)))



(defn make-run
  "Make a function that will run a factory.
  Map arg keys:
  - `execute-bbs`: function that will execute building blocks.
  - `factory->graph`: function that returns a dependency graph from a factory.
  - `compute-order`: function that compute the order in which to run the
    factory's building blocks

  `compute-order` args
  args:
  - 'factory': the factory, a map of building block ids to building blocks.
  - 'factory->graph': function returning a dependency graph from a factory.
  "
  [{:keys [execute-bbs factory->graph]}]
  (fn [factory inputs]
    (let [order (compute-order factory factory->graph)]
      (execute-bbs factory inputs order))))


;; -----------------------------------------------------------------------------
;; Minimal run function.
;; -----------------------------------------------------------------------------
(def run-factory
  (make-run {:execute-bbs execute-bbs
             :factory->graph factory->graph}))

(tests
  (def inputs
    {:a 1
     :b 3
     :c -5})

  (def ex-res
    {:d 4, :e -1, :f :no-deps,})

  (run-factory ex-factory inputs)
  := ex-res)


;; -----------------------------------------------------------------------------
;; Alternative runners
;; -----------------------------------------------------------------------------
(defn make-factory->fn
  "Build a function that turns a factory into a function.
  Input names are found in metadata under the ::input-names key."
  [{:keys [execute-bbs factory->graph get-input-names]}]
  (fn factory->fn [factory]
    (let [order (compute-order factory factory->graph)]
      (with-meta
        (fn run-map [inputs]
          (execute-bbs factory inputs order))
        {::input-names (get-input-names factory)
         ::output-names (get-bb-ids factory)}))))


(tests
  (def ex-factory->fn
    (make-factory->fn
      {:execute-bbs execute-bbs
       :factory->graph factory->graph
       :get-input-names get-input-names}))

  (def ex-fn (ex-factory->fn ex-factory))

  (meta ex-fn) := {::input-names #{:c :b :a}
                   ::output-names #{:e :d :f}}

  (ex-fn inputs) := ex-res)


(defn make-factory->bb
  "Build a function that turns a factory into a building block"
  [{:keys [factory->fn]}]
  (fn [factory]
    (let [f (factory->fn factory)
          input-names (-> f meta ::input-names)]
      {:deps input-names
       :fn f})))


(tests
  (def ex-factory->bb (make-factory->bb
                        {:factory->fn ex-factory->fn}))

  (def ex-bb (ex-factory->bb ex-factory))

  (run-factory {:res ex-bb} inputs)
  := {:res ex-res})


;; -----------------------------------------------------------------------------
;; Utilities
;; -----------------------------------------------------------------------------
(defn make-keep-bbs
  "Makes a function that keeps in a factory building blocks whose ids are given
  and the building blocks on which they transitively depend."
  [{:keys [factory->graph]}]
  (fn [factory bb-ids]
    (let [graph (factory->graph factory)
          bbs-to-keep (u/transitive-deps graph bb-ids)]
      (u/select-keys! factory bbs-to-keep))))

(def ^:private keep-bbs
  (make-keep-bbs
    {:factory->graph factory->graph}))


(tests
  (def keep-bbs-ex
    {:c1 {:deps #{:a :b}}
     :c2 {:deps #{:a :c}}
     :c3 {:deps #{:c1}}})

 (get-input-names (select-keys keep-bbs-ex #{:c3})) := #{:c1}
 (get-input-names (keep-bbs keep-bbs-ex #{:c3})) := #{:b :a})

#?(:clj
   (tests
     (def error-factory
       {:bb1 {:deps #{}
              :fn (fn [_] :bb1)}

        :bb2 {:deps #{:bb1}
              :fn (fn [_] (throw (ex-info "error" {})))}

        :bb3 {:deps #{:bb2}
              :fn (fn [_] :bb3)}})

     (run-factory error-factory {}) :throws clojure.lang.ExceptionInfo))


(comment
  (run-factory error-factory {})
  *e)
