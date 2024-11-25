(ns fr.jeremyschoffen.factory.v2.core
  (:require
    [clojure.set :as set]
    [hyperfiddle.rcf :refer [tests]]
    [fr.jeremyschoffen.factory.v2.common :as common]
    [fr.jeremyschoffen.factory.v2.core.internal :as i]))

;; TODO: Redo v1 using v2 then get rid of dependencies

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
  [{:keys [bb deps]}]
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
   :deps #{:get-deps}
   :fn common/make-factory->graph})


(def execute-bb-c
  {:doc "Make a function that executes one building block."
   :deps #{:get-deps :compute}
   :fn common/make-execute-bb})


(def execute-bbs-c
  {:doc "Building block making a factory executing function."
   :deps #{:execute-bb}
   :fn common/make-execute-bbs})


(def run-factory-c
  {:doc "Building block making a factory running function."
   :deps #{:factory->graph :execute-bbs}
   :fn common/make-run})


(def get-input-names-c
  {:doc "Building block making a function that returns the names of inputs
   given a factory."
   :deps #{:get-deps}
   :fn common/make-get-input-names})


(def factory->fn-c
  {:doc "Building block making a function that turns a factory into a
   function."
   :deps #{:execute-bbs :factory->graph :get-input-names}
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
       would be having the order `[:a :b :& :c]` to get an application such as
       (apply f (concat [(:a deps) (:b deps)] [:c deps])) "}
  apply-order i/apply-order)
 

(def ^{:argslist '([& {:as vs}])
       :doc "
       Make a map idendified as values of the building block for use in
       the [[bb]] constructor.
       "}
  values i/values)


(def ^{:argslist '([f & opts])
       :doc "
       Convenience constructor for building blocks inspired by the computation
       constructor used in v1.

       opts spec:
       - keywords: added to deps
       - sequential: added to deps
       - values: map made with [[values]] to declare deps values in the
         building block's deps
       - maps : merged into renames
       - function: last one used as custom apply
       "}
  bb i/bb)


(tests
  (def inputs
    {:a 1
     :b 3
     :c -5
     :seq [1 2 3]
     ::name :toto})

  (def ex-factory
    {:d {:deps #{:a :b}
         :fn (comp (partial apply +) vals)}


     :e {:deps #{:d :c}
         :fn (comp (partial apply +) vals)}


     :e2 {:deps #{:d :c}
          :custom-apply (apply-order [:d :c])
          :fn -}


     :e3 (bb (fn [n & ns] (apply - n ns))
             :e2 :seq
             (apply-order [:e2 :& :seq]))


     :f {:deps #{}
         :fn (fn [_] :no-deps)}


     :r {:renames {::name :name}
         :fn :name}


     :v {:values {::name :titi}
         :fn ::name}})

  (get-input-names ex-factory)
  := #{:a :b :c ::name :seq}

  (def expected-res
    {:d 4, :e -1, :f :no-deps,
     :e2 9
     :e3 3
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
  "Makes a building block that just extract a value at a key from another
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

