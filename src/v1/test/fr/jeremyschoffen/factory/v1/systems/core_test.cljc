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
(def connect-to-db (c :connect-to-db :db-conn
                      {:datomic-url :url}))


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
   {:db {:start connect-to-db}
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
(defn index-actions-order [recording]
  (into {}
        (comp
          (map :action)
          (map-indexed (fn [i action]
                         [action i])))
        recording))


(def starting-actions (index-actions-order start-record))


(def stopping-actions (index-actions-order stop-record))


(defn done-before? [index x y]
  (< (index x)
     (index y)))


(deftest ordering
  (testing "start"
    (are [x y] (done-before? starting-actions x y)
         :connect-to-db :make-handler
         :make-handler :start-server
         :start-server :make-foo))
  (testing "stop"
    (is (done-before? stopping-actions :stop-foo :close-server))))



;; -----------------------------------------------------------------------------
;; Sytem with error
;; -----------------------------------------------------------------------------

(def start-a (c :start-a :a))
(def start-b (c :start-b :b))
(def start-c (c :start-c :c :a :b))
(def start-d (c :start-d :d))

(defn start-e [arg]
  (throw (ex-info "Error starting e." {})))

(def stop-a (c :stop-a :a))
(def stop-b (c :stop-b :b))
(def stop-c (c :stop-c :c))
(def stop-d (c :stop-d :d))
(def stop-e (c :stop-e :e))



(def conf2
  {:components
   {:a {:start start-a
        :stop stop-a}
    :b {:start start-b
        :stop stop-b}
    :c {:start start-c
        :stop stop-c}
    :d {:start start-d
        :stop stop-d}
    :e {:start (s/c start-e :c :d)
        :stop stop-e}}})


(def system2 (s/system conf2))
(def started2 (try
                (s/start system2)
                (catch #?@(:clj [Exception e] :cljs [:default e])
                  e)))

(def partialy-started2 (-> started2 ex-data :partialy-started-system))

(def partial-state2 (:partial-state partialy-started2))

(def recorded-partial-stop2 (record-computations #(s/stop-partialy-started partialy-started2)))

(def partial-stop-record2 (:record recorded-partial-stop2))

(def indexed-stopping-actions2 (index-actions-order partial-stop-record2))

(def stopped-components2 (->> partial-stop-record2
                           (map :component-name)
                           set))


(deftest start-error
  (testing "system started up to error"
    (is (= (-> partial-state2 keys set) #{:a :b :c :d})))

  (testing "we don't stop the faulty component"
    (is (= stopped-components2 #{:a :b :c :d}))))
