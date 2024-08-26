(ns ^{:author "Jeremy Schoffen"
      :doc "
Common building blocks used to execute DAGs of interdependent computations.
      "}
  fr.jeremyschoffen.factory.v2.computations.common
  (:require
    [clojure.set :as s]
    [clojure.string :as string]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v2.dependencies.protocols :as p]
    [hyperfiddle.rcf :refer [tests]]))


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


(defn id? [v]
  (and (map? v)
       (-> v meta ::id)))


(defn id [v]
  (with-meta {:id v} {::id true}))


(defn generate-id []
  (name (gensym "computation-id__")))


(defn change-id [c id]
  (vary-meta c `p/id (constantly id)))


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
                     (m/pred id? !id)
                     (m/and (m/pred (complement values?))
                            (m/map-of !names-from !names-to)))
               ...)
    (let [result {:deps (s/union
                            (set !names-from)
                            (s/difference (set !deps) (set !names-to)))
                  :alias-map (zipmap !names-from !names-to)
                  :options (apply merge !opts)}]
      (if-let [id (-> !id last :id)]
        (assoc result :id id)
        result))))


(tests
  (meta (values {:a 1})) := {::values true}

  (parse-deps [:a :b])
  := {:deps #{:a :b} :alias-map {} :options nil}

  (parse-deps [{::c :c}])
  := {:deps #{::c} :alias-map {::c :c} :options nil}

  (parse-deps [(values {:d 1}) (values {:e 2})])
  := {:deps #{} :alias-map {} :options {:d 1 :e 2}}

  (parse-deps [(values {:d 1}) :b {::c :c} (values {:e 2}) :a])
  := {:deps #{:a :b ::c} :alias-map {::c :c} :options {:d 1 :e 2}})


(defn wrap-rename-keys
  "Wrap a function to be used as computation in order to manage aliases in a dependencies map."
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
  (let [{:keys [deps alias-map options id]} (parse-deps deps)]
    (-> f
      (cond->
        (seq options) (wrap-merge-value options)
        (seq alias-map) (wrap-rename-keys alias-map))
      (vary-meta  merge
        {`p/dependencies (constantly deps)
         `p/id (constantly (or id (generate-id)))
         ::computation true}))))

(tests
  (p/id (c (fn []) (id :c1))) := :c1
  (-> (p/id (c (fn [])))
      (string/starts-with? "computation-id__")) := true)
