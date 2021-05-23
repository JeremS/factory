(ns fr.jeremyschoffen.factory.v1.systems.core
  (:require
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.systems.common :as common]))


(def c r/c)


(def options r/options)


(def system common/conf->system)


(def internal-api (r/run common/impl))


(def ^:private run-pre-start (:run-pre-start internal-api))


(def ^:private run-start (:run-start internal-api))


(def ^:private run-post-start (:run-post-start internal-api))


(def ^:private run-stop (:run-stop internal-api))


(defn start [system]
  (-> system
      run-pre-start
      run-start
      run-post-start))


(defn stop [system]
  (run-stop system))





