(ns fr.jeremyschoffen.factory.v1.systems.manifold-test
  (:require
    #?(:clj [clojure.test :refer (deftest is)]
       :cljs [cljs.test :refer-macros (deftest is async)])
    #?(:clj [manifold.deferred :as manifold]
       :cljs [manifold-cljs.deferred :as manifold])))


(comment
  (defn make-example []
    {:a (manifold/deferred)
     :b 3
     :c (b/c {:deps [:a :b]
              :f (fn [m]
                   (->> m
                     vals
                     (apply +)))})})

  (defn make-res [example]
    (fm/combine-map
      (fm/execute-computations
        (select-keys example #{:a :b})
        (select-keys example #{:c})
        [:c])))


  (def expected-res
    {:a 1
     :b 3
     :c 4})


  (deftest async-execution
    (let [example (make-example)
          res (make-res example)]

      (is (not (manifold/realized? res)))
      (manifold/success! (:a example) 1)
      #?(:clj
         (is (= (deref res) expected-res))

         :cljs
         (async done
           (manifold/chain res
             (fn [v]
               (is (= v expected-res))
               (done))))))))

