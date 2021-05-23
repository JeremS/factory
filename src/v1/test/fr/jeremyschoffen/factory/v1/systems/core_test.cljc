(ns fr.jeremyschoffen.factory.v1.systems.core-test
  (:require
    #?(:clj  [clojure.test :refer (deftest testing is are)]
       :cljs [cljs.test :refer-macros (deftest testing is are)])
    [fr.jeremyschoffen.factory.v1.systems.core :as s]))

;; -----------------------------------------------------------------------------
;; Utilities to create a test system and record computations
;; -----------------------------------------------------------------------------
(def ^:dynamic *record* nil)


(defn record-computations [c]
  (binding [*record* (atom [])]
    (let [res (c)]
      {:res res
       :record @*record*})))


(defn c
  "Make a generic compuation that can record itself."
  [action-name component-name & deps]
  (apply s/c
    (fn [deps]
      (let [res [component-name deps]
            rec {:action action-name
                 :component-name component-name
                 :res res}]
        (when *record*
          (swap! *record* conj rec))
        res))
    deps))


;; -----------------------------------------------------------------------------
;; Test system def
;; -----------------------------------------------------------------------------
(def create-database (c :create-database :created-db
                        {:datomic-url :url}))


(def connect-to-db (c :connect-to-db :db-conn
                      {:datomic-url :url}))


(def seed-connection (c :seed-connection :seeded-conn))


(def make-handler (c :make-handler :web-handler
                     :db))


(def start-server (c :start-server :web-server
                     :handler))


(def close-server (c :close-server :closed-server))


(def make-foo (c :make-foo :foo
                 :http))


(def stop-foo (c :stop-foo :stopped-foo))


(def example-config
  {:inputs
   {:datomic-url "datomic:mem://newdb"}

   :components
   {:db {:pre-start create-database
         :start connect-to-db
         :post-start seed-connection}

    :handler {:start make-handler}
    :http {:start start-server
           :stop close-server}
    :foo {:start make-foo
          :stop stop-foo}}})



(def example-system (s/system example-config))



;; -----------------------------------------------------------------------------
;; Start & stop the test system while recording
;; -----------------------------------------------------------------------------
(def recorded-start (record-computations #(s/start example-system)))

(def start-record (:record recorded-start))
(def started-system (:res recorded-start))

(def recorded-stop (record-computations #(s/stop started-system)))
(def stop-record (:record recorded-stop))
(def stopped-system (:res recorded-stop))


;; -----------------------------------------------------------------------------
;; Extract the order compuations have been scheduled
;; -----------------------------------------------------------------------------
(def starting-actions
  (into {}
        (comp
          (map :action)
          (map-indexed (fn [i action]
                         [action i])))
        start-record))


(def stopping-actions
  (into {}
        (comp
          (map :action)
          (map-indexed (fn [i action]
                         [action i])))
        stop-record))


(defn done-before? [index x y]
  (< (index x)
     (index y)))


(deftest ordering
  (testing "start"
    (are [x y] (done-before? starting-actions x y)
         :create-database :connect-to-db
         :connect-to-db :make-handler
         :make-handler :start-server
         :start-server :make-foo
         :make-foo :seed-connection))
  (testing "stop"
    (is (done-before? stopping-actions :stop-foo :close-server))))

