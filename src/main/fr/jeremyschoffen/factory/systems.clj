(ns fr.jeremyschoffen.factory.systems
  (:require
    [fr.jeremyschoffen.factory.core :as c]
    [fr.jeremyschoffen.factory.graph :as g]))


(defn compute-start [bb-id bb deps current-value]
  (c/compute
    bb-id
    (assoc bb :fn (:start bb))
    deps
    current-value))


(defn compute-stop [_bb-id bb _deps current-value]
  (when current-value
    ((:stop bb identity) current-value)))


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
(def start (:run (c/run c/api-factory start-inputs)))

(def stop (:run (c/run c/api-factory stop-inputs)))


(comment
  (def system
    {:handler {:deps #{:routes :middleware}
               :start (fn [deps]
                         [:handler deps])}
     :web-server {:deps #{:handler :port}
                  :start (fn [deps] [:web-server deps])
                  :stop  (fn [deps] [:web-server {:old deps}])}})


  (def system-conf
    {:routes :dum-routes
     :middleware :dm
     :port 8080})

  (def started (start system system-conf))
  (def stopped (stop system started))


  (def start' ((:factory->fn (c/run c/api-factory start-inputs))
               system))

  (def stop' ((:factory->fn (c/run c/api-factory stop-inputs))
              system))


  (def started' (start' system-conf))
  (def stopped (stop' started')))


