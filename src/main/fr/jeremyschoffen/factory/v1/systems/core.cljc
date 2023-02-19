(ns fr.jeremyschoffen.factory.v1.systems.core
  (:require
    [clojure.set :as s]
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.systems.common :as common]))


(def c r/c)


(def values r/values)


(def system common/conf->system)


(def current-value common/current-value)


(def ^:private internal-api (r/run common/impl))


(def ^:private exec-on-deps (:execute-computations-on-deps internal-api))
(def ^:private exec-on-current-val (:execute-computations-on-current-val internal-api))


(defn start [{:keys [state phases order] :as system}]
  (let [computations (get-in phases [:start :computations])]
    (try
      (assoc system :state (exec-on-deps state computations order))
      (catch #?@(:clj [Exception e] :cljs [:default e])
        (let [partial-state (-> e ex-data :current-state)
              new-system (-> system
                           (dissoc :state)
                           (assoc :partial-state partial-state))]
          (throw (ex-info "Error while starting the system."
                          {:partially-started-system new-system}
                          e)))))))


(defn stop [{:keys [state phases order] :as system}]
  (let [phase (:stop phases)
        {:keys [computations computation-names]} phase
        order (->> order
                   (filterv computation-names)
                   rseq)]
    (exec-on-current-val state computations order)))


(defn stop-partially-started [{:keys [partial-state phases order] :as system}]
  (let [phase (:stop phases)
        {:keys [computations computation-names]} phase
        components-to-stop (s/intersection (-> partial-state keys set)
                                           computation-names)
        order (->> order
                   (filterv components-to-stop)
                   rseq)]
    (exec-on-current-val partial-state computations order)))


