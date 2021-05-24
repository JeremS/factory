(ns fr.jeremyschoffen.factory.v1.computations.common
  (:require
    [clojure.set :as s]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.factory.v1.dependencies.graph :as g]
    [fr.jeremyschoffen.factory.v1.dependencies.protocols :as p]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


;; -----------------------------------------------------------------------------
;; Computation representation
;; -----------------------------------------------------------------------------
(defn values? [m]
  (and (map? m)
       (-> m meta ::values)))


(defn values [& {:as opts}]
  (with-meta opts {::values true}))


(defn computation? [x]
  (some-> x meta ::computation))


(defn parse-deps [deps]
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


(defn wrap-rename-keys [f renames]
  (fn [deps]
    (-> deps
        (s/rename-keys renames)
        f)))


(defn wrap-merge-opts [f opts]
  (fn [m]
    (f (merge m opts))))


(defn c
  "Make a computation from a function `f`, declaring its dependencies in
  `deps`."
  [f & deps]
  (let [{:keys [deps alias-map options]} (parse-deps deps)]
    (-> f
      (cond->
        (seq options) (wrap-merge-opts options)
        (seq alias-map) (wrap-rename-keys alias-map))
      (vary-meta  merge
        {`p/dependent? (constantly true)
         `p/dependencies (constantly deps)
         ::computation true}))))


;; -----------------------------------------------------------------------------
;; System info
;; -----------------------------------------------------------------------------
(defn computation-names [computations]
  (-> computations keys set))


(defn computations-order [dependency-graph computation-names]
  (->> dependency-graph
    g/topsort
    (filterv (set computation-names))))


;; -----------------------------------------------------------------------------
;; Promise based async/parallel computation
;; -----------------------------------------------------------------------------
(defn make-combine-map [{:keys [combine then]}]
  (fn combine-map [m]
    (let [[names promises] (m/find m
                             (m/map-of !k !v)
                             [!k !v])]
      (-> (combine promises)
          (then (fn [ps]
                  (zipmap names ps)))))))


(defn make-combine-mixed-map [{:keys [combine-map promise? then make-resolved]}]
  (fn combine-mixed-map [m]
    (let [{realized-part false
           deferred-part true} (u/split-map m promise?)]
      (if (empty? deferred-part)
        (make-resolved realized-part)
        (-> deferred-part
            combine-map
            (then (fn [newly-realized-deps]
                    (merge realized-part newly-realized-deps))))))))


(defn make-gather-deps-async [{:keys [combine-mixed-map gather-deps]}]
  (fn gather-deps-async [state deps-names]
    (-> state
        (gather-deps deps-names)
        combine-mixed-map)))


(defn make-compute-async [{:keys [compute then]}]
  (fn compute-async [{:keys [deps] :as ctxt}]
    (-> deps
        (then (fn [realized-deps]
                (-> ctxt
                    (assoc :deps realized-deps)
                    compute))))))

;; -----------------------------------------------------------------------------
;; Execution fns
;; -----------------------------------------------------------------------------
(defn make-execute-computation [gather-deps compute]
  (fn execute-computation [inputs computation-name computation]
    (let [dep-names (p/dependencies computation)
          deps (gather-deps inputs dep-names)
          current-val (get inputs computation-name)]
      (compute {:computation-name computation-name
                :computation computation
                :current-value current-val
                :deps deps}))))


(defn make-execute-computations [{:keys [gather-deps compute]}]
  (let [execute-computation (make-execute-computation gather-deps compute)]
    (fn execute-computations [inputs computations order]
      (persistent!
        (reduce
          (fn [state computation-name]
            (assoc! state computation-name
              (execute-computation state
                                   computation-name
                                   (get computations computation-name))))
          (transient inputs)
          order)))))


(defn make-execute-computations-async [{:keys [execute-computations combine-mixed-map]}]
  (fn execute-computations-async [& args]
    (combine-mixed-map (apply execute-computations args))))


(defn make-run [{:keys [execute-computations split-config]}]
  (fn run [config]
    (let [{:keys [inputs computations]} (split-config config)
          computation-names (computation-names computations)
          dependency-graph (g/make-graph computations)
          order (computations-order dependency-graph computation-names)]
      (execute-computations inputs computations order))))
