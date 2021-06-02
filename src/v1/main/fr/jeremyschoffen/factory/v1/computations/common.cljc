(ns ^{:author "Jeremy Schoffen"
      :doc "
Common building blocks used to execute DAGs of interdepent computations.
      "}
  fr.jeremyschoffen.factory.v1.computations.common
  (:require
    [clojure.set :as s]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


;; -----------------------------------------------------------------------------
;; Computation representation
;; -----------------------------------------------------------------------------
(defn values?
  "Check wether a map `m` in a computation declaration is used to declare static values intead of dependencies."
  [m]
  (and (map? m)
       (-> m meta ::values)))


(defn values
  "Make a map of values to be used in the computation."
  [& {:as opts}]
  (with-meta opts {::values true}))


(defn computation?
  "Checks wether a value `x` is a computation."
  [x]
  (some-> x meta ::computation))


(defn parse-deps
  "Parse the trailing arguments of a compuation declaration."
  [deps]
  (m/find deps
    (m/seqable (m/or (m/pred keyword? !deps)
                     (m/pred sequential? (m/seqable !deps ...))
                     (m/pred values? !opts)
                     (m/and (m/pred (complement values?))
                            (m/map-of !names-from !names-to)))
               ...)
    {:deps (s/union
               (set !names-from)
               (s/difference (set !deps) (set !names-to)))
     :alias-map (zipmap !names-from !names-to)
     :options (apply merge !opts)}))


(defn wrap-rename-keys
  "Wrap a function to be used as compuation in order to manage aliases in a dependencies map."
  [f renames]
  (fn [deps]
    (-> deps
        (s/rename-keys renames)
        f)))


(defn wrap-merge-value
  "Wrap a function to be used as compuation in order to manage static values."
  [f values]
  (fn [m]
    (f (merge m values))))


(defn c
  "Make a computation from a function `f`, declaring its dependencies in
  `deps`.

  `f` must be function that takes one parameter, a map of dependency names to their values.
  `deps` can be keyword, sequences of keywords, maps from alias to dependency name or static values using [[values]]"
  [f & deps]
  (let [{:keys [deps alias-map options]} (parse-deps deps)]
    (-> f
      (cond->
        (seq options) (wrap-merge-value options)
        (seq alias-map) (wrap-rename-keys alias-map))
      (vary-meta  merge
        {`p/dependencies (constantly deps)
         ::computation true}))))


;; -----------------------------------------------------------------------------
;; System info
;; -----------------------------------------------------------------------------
(defn computation-names
  "Get computations names in a set from map of name -> computations."
  [computations]
  (-> computations keys set))


(defn computations-order
  "Get a topological sort from a dependency graph in which non computations are filtered out. This gives us a valid order in which to run the computations."
  [dependency-graph computation-names]
  (->> dependency-graph
    g/topsort
    (filterv (set computation-names))))


;; -----------------------------------------------------------------------------
;; Promise based async/parallel computation
;; -----------------------------------------------------------------------------
(defn make-combine-map
  "Make a combine-map function that will combine a map of promises into the promise of a map.

  Args: a map of 2 keys:
  - `:combine`: a function that combines a seq of promises into the promise of a seq.
  - `:then`: a 'then' function in a particular promise implementation."
  [{:keys [combine then]}]
  (fn combine-map [m]
    (let [[names promises] (m/find m
                             (m/map-of !k !v)
                             [!k !v])]
      (-> (combine promises)
          (then (fn [ps]
                  (zipmap names ps)))))))


(defn make-combine-mixed-map
  "Similar to [[make-combine-map]] except that the function created here accepts a map whose values may not be promises.

  Arg: a map with the keys:
  - `:combine-map`: a 'combine-map' function such as one made using [[make-combine-map]]
  - `:promise?`: a function telling wether a value is a promise or not
  - `:then`: a 'then' function in a particular promise implementation
  - `:make-resolved`: a function that makes a resolved promise
  "
  [{:keys [combine-map promise? then make-resolved]}]
  (fn combine-mixed-map [m]
    (let [{realized-part false
           deferred-part true} (u/split-map m promise?)]
      (if (empty? deferred-part)
        (make-resolved realized-part)
        (-> deferred-part
            combine-map
            (then (fn [newly-realized-deps]
                    (merge realized-part newly-realized-deps))))))))


(defn make-gather-deps-async
  "Wrap a `gather-deps` function with [[combine-mixed-map]]. This allows computations to pass promises of dependencies."
  [{:keys [combine-mixed-map gather-deps]}]
  (fn gather-deps-async [state deps-names]
    (-> state
        (gather-deps deps-names)
        combine-mixed-map)))


(defn make-compute-async
  "Wrap the compute function such that it accepts a the promise of a dependency map instead of the map itself."
  [{:keys [compute then]}]
  (fn compute-async [{:keys [deps] :as ctxt}]
    (-> deps
        (then (fn [realized-deps]
                (-> ctxt
                    (assoc :deps realized-deps)
                    compute))))))

;; -----------------------------------------------------------------------------
;; Execution fns
;; -----------------------------------------------------------------------------
(defn- make-execute-computation
  "Make a function that will execute one computation."
  [gather-deps compute]
  (fn execute-computation [inputs computation-name computation]
    (let [dep-names (p/dependencies computation)
          deps (gather-deps inputs dep-names)
          current-val (get inputs computation-name)]
      (compute {:computation-name computation-name
                :computation computation
                :current-value current-val
                :deps deps}))))


(defn make-execute-computations
  "Make a function that will execute computations.

  Map arg keys:
  - `gather-deps`: a function (state, dependency names) -> map of (dependency names -> dependency val)
  - `compute`: a function that will perform a computation given the dependencies, current value and the computation itself.
  "
  [{:keys [gather-deps compute]}]
  (let [execute-computation (make-execute-computation gather-deps compute)]
    (fn execute-computations [inputs computations order]
      (persistent!
        (reduce
          (fn [state computation-name]
            (let [res (try (execute-computation state
                                                computation-name
                                                (get computations computation-name))
                           (catch #?@(:clj [Exception e] :cljs [:default e])
                             (throw (ex-info (str "Error while running: " computation-name)
                                             {:current-state (persistent! state)}
                                             e))))]
              (assoc! state computation-name res)))
          (transient inputs)
          order)))))


(defn make-execute-computations-async
  "Wraps a `execute-computations` function made by [[make-execute-computations]] with a `combine-mixed-map` function."
  [{:keys [execute-computations combine-mixed-map]}]
  (fn execute-computations-async [& args]
    (combine-mixed-map (apply execute-computations args))))


(defn make-run
  "Make a function that will run a computations config."
  [{:keys [execute-computations split-config]}]
  (fn run [config]
    (let [{:keys [inputs computations]} (split-config config)
          computation-names (computation-names computations)
          dependency-graph (g/make-graph computations)
          order (computations-order dependency-graph computation-names)]
      (execute-computations inputs computations order))))
