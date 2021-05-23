(ns fr.jeremyschoffen.factory.v1.systems.core
  (:require
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.systems.common :as common]))


(def c r/c)
(def system common/conf->system)


(def internal-api
  (r/run common/impl))


(def ^:private execute-on-deps (:execute-computations-on-deps internal-api))


(def ^:private execute-on-current-val (:execute-computations-on-current-val internal-api))



(defn make-run-phase [{:keys [execute-computations phase-name keep-state reverse-order]
                       :or {keep-state true reverse-order false}}]
  (fn [{:keys [state phases order] :as system}]
    (let [phase (get phases phase-name)
          {:keys [computations computation-names]} phase
          order (cond-> (filterv computation-names order)
                  reverse-order rseq)
          new-state (execute-computations state computations order)]
       (cond-> system
         keep-state (assoc :state new-state)))))



(def ^:private run-pre-start
  (make-run-phase {:execute-computations execute-on-deps
                   :phase-name :pre-start
                   :keep-state false
                   :reverse-order false}))


(def ^:private run-start
  (make-run-phase {:execute-computations execute-on-deps
                   :phase-name :start
                   :keep-state true
                   :reverse-order false}))


(def ^:private run-post-start
  (make-run-phase {:execute-computations execute-on-current-val
                   :phase-name :post-start
                   :keep-state false
                   :reverse-order false}))


(def ^:private run-stop
  (make-run-phase {:execute-computations execute-on-current-val
                   :phase-name :stop
                   :keep-state false
                   :reverse-order true}))



(defn start [system]
  (-> system
      run-pre-start
      run-start
      run-post-start))


(defn stop [system]
  (run-stop system))





