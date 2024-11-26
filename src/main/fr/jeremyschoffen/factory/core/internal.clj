(ns fr.jeremyschoffen.factory.core.internal
  (:require
    [clojure.set :as set]
    [hyperfiddle.rcf :refer [tests]]))


(defn values?
  "Check wether a map `m` in a building block declaration is used to declare
  static values intead of dependencies."
  [m]
  (and (map? m)
       (-> m meta ::values)))


(defn values
  "Make a map of values to be used in the computation."
  [& {:as opts}]
  (with-meta opts {::values true}))


(defn- into! [transient-coll values]
  (reduce
    (fn [acc v]
      (conj! acc v))
    transient-coll
    values))


(defn parse-deps [xs]
  (let [deps    (volatile! (transient #{}))
        renames (volatile! (transient {}))
        values  (volatile! (transient {}))
        custom-apply (volatile! nil)]
    (doseq [x xs]
      (cond
        (keyword? x)    (vswap! deps conj! x)
        (sequential? x) (vswap! deps into! x)
        (values? x)     (vswap! values into! x)
        (map? x)        (vswap! renames into! x)
        (fn? x)         (vreset! custom-apply x)))
    (let [deps         (-> deps deref persistent!)
          renames      (-> renames deref persistent!)
          values       (-> values deref persistent!)
          custom-apply (deref custom-apply)]
      (cond-> {:deps (set/union (set (keys renames))
                                (set/difference deps
                                                (set (vals renames))
                                                (set (keys values))))}
        (seq renames) (assoc :renames renames)
        (seq values)  (assoc :values values)
        custom-apply  (assoc :custom-apply custom-apply)))))


(tests
  "We can discriminate value maps"
  (meta (values {:a 1})) := {::values true}

  "Simple sequential deps"
  (parse-deps [:a :b])
  := {:deps #{:a :b}}

  "Map deps"
  (parse-deps [{::c :c}])
  := {:deps #{::c} :renames {::c :c}}

  "Merging values"
  (parse-deps [(values {:d 1}) (values {:e 2})])
  := {:deps #{} :values {:d 1 :e 2}}

  "Values override deps"
  (parse-deps [:d (values {:d 1})])
  := {:deps #{}, :values {:d 1}}

  (parse-deps [(values {:d 1}) :b {::c :c} (values {:e 2}) :a])
  := {:deps #{:a :b ::c} :renames {::c :c} :values {:d 1 :e 2}}


  (defn- custom-apply [f m]
    (apply f (vals m)))


  (parse-deps [:d (values {:d 1}) :b {::c :c} (values {:e 2}) :a custom-apply])
  := {:deps #{:a :b ::c}
      :renames {::c :c}
      :values {:d 1 :e 2}
      :custom-apply custom-apply})


(defn bb
  "Simplified constructor for a building block.

  Args:
  - `f`: the `:fn` value for the building block
  - `deps`: list of opts
  "
  [f & deps]
  (let [deps (parse-deps deps)]
    (assoc deps :fn f)))


(defn- extract-args
  "Arrange the dependencies from a `deps` map into a vector according to the
  provided `order`."
  [deps order]
  (mapv (fn [dep]
          (get deps dep))
        order))


(defn apply-order
  "Make a custom apply function that will apply a single map to a function
  instead of a sequence of values with [[apply]]."
  [order]
  (fn [f deps]
    (let [args (extract-args deps order)]
      (apply f args))))


(defn bb-apply [f & args]
  (let [custom-apply (apply-order args)
        deps (remove #{:&} args)]
    (bb f deps custom-apply)))

