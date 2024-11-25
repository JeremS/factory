(ns fr.jeremyschoffen.factory.v2.core.internal
  (:require
    [clojure.set :as set]
    [fr.jeremyschoffen.factory.v2.utils :as u]
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
    (let [deps (-> deps deref persistent!)
          renames (-> renames deref persistent!)
          values (-> values deref persistent!)
          custom-apply (deref custom-apply)]
      (cond-> {:deps (set/union (set (keys renames))
                                (set/difference (set deps)
                                                (set (vals renames))))}
        (seq renames) (assoc :renames renames)
        (seq values)  (assoc :values values)
        custom-apply  (assoc :custom-apply custom-apply)))))


(tests
  (meta (values {:a 1})) := {::values true}

  (parse-deps [:a :b])
  := {:deps #{:a :b}}

  (parse-deps [{::c :c}])
  := {:deps #{::c} :renames {::c :c}}

  (parse-deps [(values {:d 1}) (values {:e 2})])
  := {:deps #{} :values {:d 1 :e 2}}

  (parse-deps [(values {:d 1}) :b {::c :c} (values {:e 2}) :a])
  := {:deps #{:a :b ::c} :renames {::c :c} :values {:d 1 :e 2}}


  (def ca (fn [f m]
            (apply f (vals m))))

  (parse-deps [(values {:d 1}) :b {::c :c} (values {:e 2}) :a ca])
  := {:deps #{:a :b ::c}
      :renames {::c :c}
      :values {:d 1 :e 2}
      :custom-apply ca})


(defn bb [f & deps]
  (let [deps (parse-deps deps)]
    (assoc deps :fn f)))


(defn- parse-args [order]
  (let [[args trailing & r] (u/split-seq-on :& order)
        [trailing-arg-name & names] trailing]
    (when r
      (throw
        (ex-info
          "Error parsing apply order, more than one group of trailing args."
          {:apply-order order})))

    (when names
      (throw
        (ex-info
          "Error parsing apply-order, more than 1 arg in trailing args"
          {:apply-order order})))

    {:arg-names args
     :trailing-arg-name trailing-arg-name}))


(defn- extract-args
  [deps order]
  (let [{:keys [arg-names trailing-arg-name] :as o}
        (parse-args order)

        args (map #(get deps %) arg-names)]
    (cond-> args
      trailing-arg-name (concat (get deps trailing-arg-name)))))


(defn apply-order
  "Returns function that will apply a argument map to a function that take
  multiple arguments.

  The application will follow the pattern given with `order`. An example
  would be having the order `[:a :b :& :c]` to get an application such as
  (apply f (concat [(:a deps) (:b deps)]
                   (:c deps))"
  [order]
  (fn [f deps]
    (let [args (extract-args deps order)]
      (apply f args))))



