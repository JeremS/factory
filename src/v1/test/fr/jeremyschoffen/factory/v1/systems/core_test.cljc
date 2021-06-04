(ns fr.jeremyschoffen.factory.v1.systems.core-test
  (:require
    #?(:clj  [clojure.test :refer (deftest testing is are)]
       :cljs [cljs.test :refer-macros (deftest testing is are)])
    [fr.jeremyschoffen.factory.v1.systems.core :as s]))

;; -----------------------------------------------------------------------------
;; Utilities to create a test system and record computations
;; -----------------------------------------------------------------------------
(defn uuid []
  #?(:clj (java.util.UUID/randomUUID)
     :cljs (random-uuid)))


(def ^:dynamic *record* nil)


(defn record-computations [c]
  (binding [*record* (atom [])]
    (let [res (c)]
      {:res res
       :record @*record*})))


(defn c
  "Make a generic computation that can record itself."
  [action-name component-name & deps]
  (apply s/c
    (fn [deps]
      (let [res (if-let [this (s/current-value deps)]
                  this
                  [(uuid) component-name deps])
            rec {:action action-name
                 :component-name component-name
                 :res res}]
        (when *record*
          (swap! *record* conj rec))
        res))
    deps))


(defn index-actions-order [recording]
  (into {}
        (comp
          (map :action)
          (map-indexed (fn [i action]
                         [action i])))
        recording))


(defn done-before? [index x y]
  (< (index x)
     (index y)))



;; -----------------------------------------------------------------------------
;; Definition of a test system
;; -----------------------------------------------------------------------------
(def start-a (c :start-a :a))
(def start-b (c :start-b :b))
(def start-c (c :start-c :c :a :b))
(def start-d (c :start-d :d))
(def start-e (c :start-e :e :c :d))



(def stop-c (c :stop-c :c))
(def stop-d (c :stop-d :d))
(def stop-e (c :stop-e :e))

(def conf
  {:components
   {:a {:start start-a}
    :b {:start start-b}
    :c {:start start-c
        :stop stop-c}
    :d {:start start-d
        :stop stop-d}
    :e {:start start-e
        :stop stop-e}}})

;; -----------------------------------------------------------------------------
;; Testing classic start then stop
;; -----------------------------------------------------------------------------
(def system (s/system conf))
(def recorded-start (record-computations #(s/start system)))
(def recorded-stop (record-computations #(-> recorded-start :res s/stop)))


(def starting-actions (-> recorded-start :record index-actions-order))
(def stopping-actions (-> recorded-stop :record index-actions-order))

(deftest ordering
  (testing "start"
    (are [x y] (done-before? starting-actions x y)
         :start-a :start-c
         :start-b :start-c
         :start-d :start-e
         :start-c :start-e))
  (testing "stop"
    (are [x y] (done-before? stopping-actions x y)
             :stop-e :stop-c
             :stop-e :stop-d)))

;; -----------------------------------------------------------------------------
;; Testing idempotency
;; -----------------------------------------------------------------------------
(def first-start (s/start system))
(def second-start (s/start first-start))
(def third-start (s/start second-start))


(deftest idempotency
  (is (= (-> first-start :state) (-> second-start :state)))
  (is (= (-> first-start :state) (-> third-start :state))))
;; -----------------------------------------------------------------------------
;; Testing component restriction
;; -----------------------------------------------------------------------------
(def restricted-system (s/system conf [:a :c]))
(def restricted-recorded-start (record-computations #(s/start restricted-system)))
(def restricted-recorded-stop (record-computations #(-> restricted-recorded-start :res s/stop)))

(def restricted-starting-actions (-> restricted-recorded-start :record index-actions-order))
(def restricted-stopping-actions (-> restricted-recorded-stop :record index-actions-order))


(deftest resticted-case
  (testing "started all the necessary components in order"
    (are [x y] (done-before? restricted-starting-actions x y)
         :start-a :start-c
         :start-b :start-c))
  (testing "stopped the necessary component"
    (is (contains? restricted-stopping-actions :stop-c))))


;; -----------------------------------------------------------------------------
;; Testing error case
;; -----------------------------------------------------------------------------
(defn start-e-error [_]
  (throw (ex-info "Error starting e." {})))


(def conf-error (assoc-in conf [:components :e :start] (s/c start-e-error :c :d)))

(def system-error (s/system conf-error))

(def started-error (try
                     (s/start system-error)
                     (catch #?@(:clj [Exception e] :cljs [:default e])
                       e)))

(def partialy-started (-> started-error ex-data :partialy-started-system))

(def partial-state (:partial-state partialy-started))

(def recorded-partial-stop (record-computations #(s/stop-partialy-started partialy-started)))

(def partial-stop-record (:record recorded-partial-stop))

(def partial-stopped-components (->> partial-stop-record
                                  (map :component-name)
                                  set))


(deftest error-case
  (testing "system is started up to error"
    (is (= (-> partial-state keys set) #{:a :b :c :d})))

  (testing "we don't stop the faulty component"
    (is (= partial-stopped-components #{:c :d}))))

