(ns ^{:author "Jeremy Schoffen"
      :doc "
Utility functions.
      "}
  fr.jeremyschoffen.factory.v1.utils
  (:require
    [meander.epsilon :as m]))

(defn split-map [m f]
  (let [tuples (m/search m
                 {?k (m/and ?v
                       (m/app f ?r))}
                 [[?r ?k] ?v])]
    (reduce
      (fn [acc [path v]]
        (assoc-in acc path v))
      {}
      tuples)))

(comment
  (split-map {:a 1 :b 2 :c 3}
             (fn [v]
               (if (even? v)
                 :even
                 :odd))))

