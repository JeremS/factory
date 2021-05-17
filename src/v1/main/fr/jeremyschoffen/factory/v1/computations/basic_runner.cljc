(ns fr.jeremyschoffen.factory.v1.computations.basic-runner
  (:require
    [clojure.set :as s]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as d]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as g]
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
  "Make a computation"
  [f & deps]
  (let [[deps names-from names-to] (parse-deps deps)]
    (-> f
      (wrap-rename-keys (zipmap names-from names-to))
      (vary-meta  merge 
        {`p/dependent? (constantly true)
         `p/dependencies (constantly (s/union (s/difference (set deps) 
                                                            (set names-to))
                                              (set names-from)))
         ::computation true}))))

(comment 
  (-> identity 
    (c :a [:b :c] [:a] {:toto :c})
    (p/dependencies)))

(defn computation? [x]
  (some-> x meta ::computation))





(defn compute [computation _ deps]
  (computation deps))


(def execute-computation
  (g/make-execute-computation
    {:gather-deps select-keys
     :compute compute}))


(def execute-computations
  (g/make-execute-computations
    {:execute-computation execute-computation}))


(defn split-config [m]
  (u/split-map m (fn [x]
                   (if (computation? x)
                     :computations
                     :inputs))))


(defn computation-order [graph computation-names]
  (->> graph
    (d/topsort)
    (filterv (set computation-names))))


(defn run [config]
  (let [{:keys [inputs computations]} (split-config config)
        computation-names (-> computations keys set)
        dependency-graph (d/make-graph computations)
        order (computation-order dependency-graph computation-names)]
    (execute-computations inputs computations order)))


(comment
  (def config
    {:a 1
     :b 3
     :c -5

     :d (c (comp (partial apply +) vals)
           [:a :b])
     :e (c (comp (partial apply +) vals)
           [:c :d])})

  (run config))


