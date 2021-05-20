(ns fr.jeremyschoffen.factory.v1.systems.core
  (:require
    [fr.jeremyschoffen.factory.v1.computations.basic-runner :as r]
    [fr.jeremyschoffen.factory.v1.systems.common :as common]))

(def c r/c)




(defn make-c [name]
  (fn [deps]
    (println "Computing: " name)
    (println "Map arg: " deps)
    [name deps]))

(def create-database (c (make-c :create-database) {:datomic-url :url}))
(def connect-to-db (c (make-c :connect-to-db) {:datomic-url :url}))
(def seed-connection (c (make-c :seed-connection)))
(def make-handler (c (make-c :make-handler) :db))
(def start-server (c (make-c :start-server) :handler))
(def close-server (c (make-c :close-server)))
(def make-foo (c (make-c :make-foo) :http))

(def example
  {:inputs
   {:datomic-url "datomic:mem://newdb"}

   :components
   {:db {:pre-start create-database}
        :start connect-to-db
        :post-start seed-connection

    :handler {:start make-handler}
    :http {:start start-server
           :stop close-server}
    :foo {:start make-foo}}})

(common/conf->system example)
;;TODO: create the internal api with the ns common
(def internal-api
  (r/run common/impl))


(def execute-start-like (:execute-computations-start-like internal-api))


(def execute-stop-like (:execute-computations-stop-like internal-api))



(defn make-run-phase [{:keys [execute-computations
                              phase-name
                              keep-state
                              reverse-order]
                       :or {keep-state true
                            reverse-order false}}]
  (fn [{:keys [state phases] :as system}]
    (let [{:keys [computations order]} (get phases phase-name)
          order (cond-> order
                  reverse-order rseq)
          new-state (execute-computations state computations order)]
       (cond-> system
         keep-state (assoc :state new-state)))))



(def ^:private pre-start
  (make-run-phase {:execute-computations execute-start-like
                   :phase-name :pre-start
                   :keep-state false
                   :reverse-order false}))



(defn pre-start [{:keys [state phases] :as system}]
  (let [{:keys [computations order]} (:pre-start phases)
        _ (execute-start-like state computations order)]
    system))




(defn start [{:keys [computations order initial-state] :as system}]
  (let []))


(defn stop [system])
