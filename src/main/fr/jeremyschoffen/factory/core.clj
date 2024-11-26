(ns fr.jeremyschoffen.factory.core
  (:require
    [clojure.set :as set]
    [fr.jeremyschoffen.factory.common :as common]
    [fr.jeremyschoffen.factory.graph :as g]
    [fr.jeremyschoffen.factory.core.internal :as i]
    [fr.jeremyschoffen.factory.utils :as u]
    [hyperfiddle.rcf :refer [tests]]))

;; -----------------------------------------------------------------------------
;; Getting Deps
;; -----------------------------------------------------------------------------
(defn get-deps
  "Get the deps of a building block using the `:deps` keys as well as the
  `:renames` key."
  [{:keys [deps renames values]}]
  (-> deps
      set
      (set/difference (-> values keys set)
                      (-> renames vals set))
      (set/union (-> renames keys set))))

(tests
  (get-deps {:deps #{:a :c}
             :renames {::b :b}
             :values {:c 1}})
  := #{:a ::b})


;; -----------------------------------------------------------------------------
;; Compute function
;; -----------------------------------------------------------------------------
(defn compute
  "The compute function used in this implementation.

  Allows for the use of the `:renames`, `values`, `apply-order` keys in
  building blocks definitions."
  [_bb-id bb deps _current-value]
  (let [{f :fn
         :keys [renames values custom-apply]} bb
        deps (cond-> deps
               renames (set/rename-keys renames)
               values  (merge values))]
    (if custom-apply
      (custom-apply f deps)
      (f deps))))


;; -----------------------------------------------------------------------------
;; Building blocks used to make an api
;; -----------------------------------------------------------------------------
(def factory->graph-c
  {:doc "Building block making a dependency graph constructing function."
   :deps #{:get-deps :digraph}
   :fn common/make-factory->graph})


(def execute-bb-c
  {:doc "Make a function that executes one building block."
   :deps #{:get-deps :gather-deps :compute}
   :fn common/make-execute-bb})


(def execute-bbs-c
  {:doc "Building block making a factory executing function."
   :deps #{:execute-bb}
   :fn common/make-execute-bbs})


(def run-factory-c
  {:doc "Building block making a factory running function."
   :deps #{:factory->graph :graph->order :execute-bbs}
   :fn common/make-run})


(def get-input-names-c
  {:doc "Building block making a function that returns the names of inputs
   given a factory."
   :deps #{:get-deps}
   :fn common/make-get-input-names})


(def factory->fn-c
  {:doc "Building block making a function that turns a factory into a
   function."
   :deps #{:execute-bbs :factory->graph :graph->order :get-input-names}
   :fn common/make-factory->fn})


(def factory->bb-c
  {:doc "Building block making a function that turns a factory into a building
   block."
   :deps #{:factory->fn}
   :fn common/make-factory->bb})


;; -----------------------------------------------------------------------------
;; Generated API
;; -----------------------------------------------------------------------------
(def api-inputs
  {:get-deps get-deps
   :gather-deps u/select-keys!
   :digraph g/digraph
   :graph->order g/topsort
   :compute compute})


(def api-factory
  {:factory->graph factory->graph-c
   :execute-bb execute-bb-c
   :execute-bbs execute-bbs-c
   :get-input-names get-input-names-c
   :run run-factory-c
   :factory->fn factory->fn-c
   :factory->bb factory->bb-c})


(def api (common/run-factory api-factory api-inputs))


(def ^{:arglists '([factory])
       :doc "
       Returns the names of all non building block deps from a factory.
       "}
  get-input-names (:get-input-names api))


(def ^{:argslists '([factory])
       :doc "
       Returns the dependency graph constructed from a factory.
       "}
  factory->graph (:factory->graph api))


(def ^{:arglists '([factory inputs])
       :doc "
       Run a `factory` with given `inputs`.
       Building block spec:
       - `:deps`: set of dependencies
       - `:renames`: map of input-key -> param-key
       - `:values`: map of values overrinding input values
       - `:custom-apply`: function that applies the factory's fn to the
         computated deps map
       "}
  run (:run api))


(def ^{:argslist '([factory])
       :doc "
       Builds from a factory a function that will run it given an
       inputs map.
       "}
  factory->fn (:factory->fn api))


(def ^{:argslist '([factory])
       :doc "
       Builds a building block from a factory .
       "}
  factory->bb (:factory->bb api))


;; -----------------------------------------------------------------------------
;; Utilities
;; -----------------------------------------------------------------------------
(def ^{:argslist '([order])
       :doc "
       Returns function that will apply a argument map to a function that take
       multiple arguments.

       The application will follow the pattern given with `order`. An example
       would be having the order `[:a :b :c]` to get an application such as
       (apply f (list (:a deps) (:b deps) (:c deps))) "}
  apply-order i/apply-order)


(def ^{:argslist '([& {:as vs}])
       :doc "
       Make a map idendified as values of the building block for use in
       the [[bb]] constructor.
       "}
  values i/values)


(def ^{:argslist '([f & opts])
       :doc "
       Convenience constructor for building blocks.

       opts spec:
       - keywords: added to deps
       - sequential: added to deps
       - values: map made with [[values]] to declare deps values in the
         building block's deps
       - maps : merged into renames
       - function: last one used as custom apply

       Calling:
       (bb f :a :b {::c :c} (values {:a 1} (apply-order :a :b :c)))

       Returns
       {:deps #{::c :b},
        :renames {::c :c},
        :values {:a 1},
        :custom-apply (apply-order [:a :b :c])
        :fn f})
       "}
  bb i/bb)


(def ^{:argslists '([f & deps-names])
       :doc "
       Convenience function to write simple buiding blocks using regular functions
       (as opposed to 1 map arg functions).
 
       `(bb-apply + :a :b)` is equivalent to `(bb + :a :b (apply-order [:a :b]))`
  
       `args` are what you'd put into the apply-order vector. Other conveniences
       from [[bb]] like renames aren't supported. You can add them modifying the resulting
       building block from this function though.
  "}
  bb-apply i/bb-apply)


(tests
  (def inputs
    {:a 1
     :b 3
     :c -5
     ::name :toto})

  (def ex-factory
    {:d {:deps #{:a :b}
         :fn (comp (partial apply +) vals)}


     :e {:deps #{:d :c}
         :fn (comp (partial apply +) vals)}


     :e2 (bb-apply - :d :c)

     :f {:deps #{}
         :fn (fn [_] :no-deps)}


     :r {:renames {::name :name}
         :fn :name}


     :v {:values {::name :titi}
         :fn ::name}})

  (get-input-names ex-factory)
  := #{:a :b :c ::name}

  (def expected-res
    {:d 4, :e -1, :f :no-deps,
     :e2 9
     :r :toto
     :v :titi})

  (run ex-factory inputs)
  := expected-res

  (def ex-fn (factory->fn ex-factory))

  (ex-fn inputs)
  := expected-res

  (run {:res (factory->bb ex-factory)} inputs)
  := {:res expected-res})


;; -----------------------------------------------------------------------------
;; Utilities
;; -----------------------------------------------------------------------------
(defn ->getter
  "Makes a building block that just extract a value at a key `k` from another
  building block's result."
  [dep k]
  {:deps #{dep}
   :fn (fn [m]
         (get-in m [dep k]))})


(defn inputs->factory
  "Turns an input map into a factory of dependency-less building blocks."
  [input-map]
  (update-vals input-map
               (fn [v] {:deps #{}
                        :fn (constantly v)})))

(tests
  (def merged (merge ex-factory (inputs->factory inputs)))

  (run merged {}) := (merge inputs expected-res))

