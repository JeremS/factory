(ns fr.jeremyschoffen.factory.v1.systems.promesa-test
  (:require
    #?(:clj [clojure.test :refer (deftest is)]
       :cljs [cljs.test :refer-macros (deftest is async)])
    [promesa.core :as promesa]))

(comment
  (defn make-example []
    {:a (promesa/deferred)
     :b 3
     :c (b/c {:deps [:a :b]
              :f (fn [m]
                   (->> m
                     vals
                     (apply +)))})})

  (defn make-res [example]
    (fp/combine-map
      (fp/execute-computations
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

      #?(:clj (is (not (promesa/done? res))))

      (promesa/resolve! (:a example) 1)

      #?(:clj
         (is (= (deref res) expected-res))

         :cljs
         (async done
           (promesa/then res
             (fn [v]
               (is (= v expected-res))
               (done))))))))

