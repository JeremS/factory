(ns fr.jeremyschoffen.factory.v1.systems.core
  (:require
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.systems.common :as common]))


(def c r/c)


(def values r/values)


(def system common/conf->system)


(def internal-api (r/run common/impl))


(def start (:run-start internal-api))


(def stop (:run-stop internal-api))









