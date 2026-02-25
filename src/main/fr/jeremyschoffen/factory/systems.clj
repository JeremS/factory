(ns fr.jeremyschoffen.factory.systems
  (:require
    [fr.jeremyschoffen.factory.core :as c]
    [fr.jeremyschoffen.factory.graph :as g]))

;; -----------------------------------------------------------------------------
;; Factory setup to run systems
;; -----------------------------------------------------------------------------

(defn compute-start [_bb-id bb deps _current-value]
  (let [start (:start! bb)]
    (->> deps
         (c/apply-deps-options bb)
         start)))


(defn compute-stop [_bb-id bb _deps current-value]
  (when current-value
    ((:stop! bb identity) current-value)))


(def start-inputs
  (assoc c/api-inputs
         :compute compute-start))


(defn reverse-topsort [graph]
  (when-let [s (into [] (g/topsort graph))]
    (rseq s)))



(def stop-inputs
  (assoc c/api-inputs
         :gather-deps (constantly nil)
         :compute compute-stop
         :graph->order reverse-topsort))


;; basic
(let [start* (:run (c/run c/api-factory start-inputs))
      stop*  (:run (c/run c/api-factory stop-inputs))]
  (defn start!
    "Start a system from system-factory and an input map.

    Return value:
    The started system.
    "
    [factory inputs]
    (start* factory inputs))


  (defn stop!
    "Stops a system."
    ([factory system]
     (stop* factory system))))



(comment
  (def system
    {:handler {:deps #{:routes :middleware}
               :renames {::routes :routes}
               :start! (fn [deps] [:handler deps])}
     :web-server {:deps #{:handler :port}
                  :start! (fn [deps] [:web-server deps])
                  :stop!  (fn [this] [:web-server {:old this}])}})


  (def system-conf
    {::routes :dum-routes
     :middleware :dm
     :port 8080})

  (def started (start! system system-conf))
  (def stopped (stop! system started)))


;; -----------------------------------------------------------------------------
;; System / reloaded workflow
;; -----------------------------------------------------------------------------
(defn- try-thunk [thunk]
  (try
    (thunk)
    (catch Exception e e)))


(defn- exception? [x]
  (instance? Exception x))

(defn ->system [system-factory inputs]
  {:inputs inputs
   :factory system-factory})

(defn ->!system
  "Returns a system which is an atom containing all the data to start and stop
  the `system-factory` with the given `inputs`."
  [system-factory inputs]
  (atom (->system system-factory inputs)))

(defn swap-factory! [!system f]
  (swap! !system assoc :factory f))


(defn swap-inputs [!system i]
  (swap! !system :inputs i))


(defn get-bbs-state [!system]
  (-> !system deref :bbs-state))


(defn- format-system [system-map lifecycle-result ok-state error-state]
  (let [error? (exception? lifecycle-result)
        bbs-state (-> lifecycle-result
                      (cond-> error? c/ex->current-state)
                      (as-> $ (apply dissoc $ (-> system-map :inputs keys))))]
    (merge system-map
      (if error?
        {:bbs-state bbs-state :state error-state :error lifecycle-result}
        {:bbs-state bbs-state :state ok-state    :error nil}))))


(defn- run-lifecycle! [!system run? lifecycle-fn]
  (let [[old new] (swap-vals! !system #(cond-> % (run? %) lifecycle-fn))]
    (if (identical? old new)
      :no-op
      (:state new))))


(def startable-states #{nil :stopped})

(defn startable? [system-map]
  (contains? startable-states (:state system-map)))


(defn- -start-system! [system-map]
  (let [{:keys [factory inputs]} system-map
        lifecycle-result (try-thunk #(start! factory  inputs))]
    (format-system system-map lifecycle-result :started :partially-started)))


(defn start-system!
  "Starts a `!system` created with [[->!system]].

  Return:
  Either the new system value or `:no-op` when the system can't be started"
  [!system]
  (run-lifecycle! !system startable? -start-system!))


(def stoppable-states #{:started :partially-started :partially-stopped})

(defn stoppable? [system-map]
  (contains? stoppable-states (:state system-map)))


(defn- -stop-system! [system-map]
  (let [{:keys [factory bbs-state]} system-map
        lifecycle-result (try-thunk #(stop! factory bbs-state))]
    (format-system system-map lifecycle-result :stopped :partially-stopped)))


(defn stop-system!
  "Stops a `!system` created with [[->!system]].

  Return:
  Either the new system value or `:no-op` when the system can't be stopped."
  [!system]
  (run-lifecycle! !system stoppable? -stop-system!))

(defn- stopped? [x]
  (contains? #{:no-op :stopped} x))

(defn restart-system!
  ([!system]
   (let [stop-res (stop-system! !system)]
     (if (stopped? stop-res)
       (start-system! !system)
       stop-res)))
  ([!system factory inputs]
   (let [stop-res (stop-system! !system)]
     (if-not (stopped? stop-res)
       stop-res
       (do
         (reset! !system (->system factory inputs))
         (start-system! !system))))))



(comment
  (do
    (def factory
      {:handler {:deps #{:routes :middleware}
                 :renames {::routes :routes}
                 :start! (fn [deps] [:handler deps])}
       :web-server {:deps #{:handler :port}
                     :start! (fn [deps] [:web-server deps])
                     :stop!  (fn [this] [:web-server {:old this}])}})


    (def inputs
      {::routes :dum-routes
       :middleware :dm
       :port 8080})

    (def !system (->!system factory inputs)))
  (start-system! !system)
  (restart-system! !system factory inputs)
  (stop-system! !system))




