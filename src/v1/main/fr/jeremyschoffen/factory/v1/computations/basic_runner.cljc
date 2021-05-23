(ns fr.jeremyschoffen.factory.v1.computations.basic-runner
  (:require
    [fr.jeremyschoffen.factory.v1.computations.common :as common]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


(def c common/c)


(def options common/options)


(defn compute [{:keys [computation deps]}]
  (computation deps))


(def execute-computations
  (common/make-execute-computations
    {:gather-deps select-keys
     :compute compute}))


(defn split-config [m]
  (u/split-map m (fn [x]
                   (if (common/computation? x)
                     :computations
                     :inputs))))


(def run
  (common/make-run
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


