(ns fr.jeremyschoffen.factory.v1.computations.basic-runner
  (:require
    [clojure.set :as s]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]
    [fr.jeremyschoffen.factory.v1.utils :as u]))



(defn- parse-deps [deps]
  (m/find deps
      (m/seqable (m/or (m/pred keyword? !k)
                       (m/pred sequential? (m/seqable !k ...))
                       (m/map-of !x !y))
                 ...)
      [!k !x !y]))


(defn wrap-rename-keys [f renames]
  (fn [deps]
    (-> deps
        (s/rename-keys renames)
        f)))


(defn c
  "Make a computation from a function `f`, declaring its dependencies in
  `deps`."
  [f & deps]
  (let [[deps names-from names-to] (parse-deps deps)
        f (cond-> f
            (seq names-from)
            (wrap-rename-keys (zipmap names-from names-to)))]
    (-> f
      (vary-meta  merge
        {`p/dependent? (constantly true)
         `p/dependencies (constantly (s/union (s/difference (set deps)
                                                            (set names-to))
                                              (set names-from)))
         ::computation true}))))


(defn computation? [x]
  (some-> x meta ::computation))


(defn compute [{:keys [computation deps]}]
  (computation deps))


(def execute-computation
  (bb/make-execute-computation
    {:gather-deps select-keys
     :compute compute}))


(def execute-computations
  (bb/make-execute-computations
    {:execute-computation execute-computation}))


(defn split-config [m]
  (u/split-map m (fn [x]
                   (if (computation? x)
                     :computations
                     :inputs))))


(def run
  (bb/make-run
    {:execute-computations execute-computations
     :split-config split-config}))


(comment
  (def config
    {:a 1
     :b 3
     ::c -5

     :d (c (comp (partial apply +) vals)
           [:a :b])
     :e (c (comp (partial apply +) vals)
           :d
           {::c :c})})

  (run config))


