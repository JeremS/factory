(ns fr.jeremyschoffen.factory.v1.computations.basic-runner
  (:require
    [fr.jeremyschoffen.factory.v1.computations.building-blocks :as bb]
    [fr.jeremyschoffen.factory.v1.utils :as u]))


(def c bb/c)


(def options bb/options)


(defn compute [{:keys [computation deps]}]
  (computation deps))


(def execute-computations
  (bb/make-execute-computations
    {:gather-deps select-keys
     :compute compute}))


(defn split-config [m]
  (u/split-map m (fn [x]
                   (if (bb/computation? x)
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


