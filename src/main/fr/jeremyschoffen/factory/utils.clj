(ns ^{:author "Jeremy Schoffen"
      :doc "
Utility functions.
      "}
  fr.jeremyschoffen.factory.utils
  (:require
    [hyperfiddle.rcf :refer [tests]]))


;; -----------------------------------------------------------------------------
;; Map utils
;; -----------------------------------------------------------------------------
(defn un-zipmap [m]
  (let [ks (volatile! (transient []))
        vs (volatile! (transient []))]
    (doseq [[k v] m]
      (vswap! ks conj! k)
      (vswap! vs conj! v))
    [(-> ks deref persistent!)
     (-> vs deref persistent!)]))

(tests
  (def unzip-ex {:a 1 :b 2})

  (->> unzip-ex
       un-zipmap
       (apply zipmap)) := unzip-ex)


(defn split-map-kv
  "Return a map of maps whose keys are determinded using the function `f` on
  map entries of `m` such as:
  (fn f [[k v]] value-to-group-kvs)

  The values of the result are map whose map entries have the same values by `f` "
  [m f]
  (-> m
      (->> (group-by f))
      (update-vals #(into {} %))))


(defn split-map-by-val
  "Split a map by appling a discrimination function only to its values."
  [m f]
  (split-map-kv m (fn [[_ v]]
                    (f v))))

(tests
  (defn split-even-odd [v]
    (if (even? v) :even :odd))


  (split-map-by-val {:a 1 :b 2 :c 3} split-even-odd)
  := {:odd {:a 1, :c 3}, :even {:b 2}})


(defn split-map-by-key
  "Split a map by appling a discrimination function only to its keys."
  [m f]
  (split-map-kv m (fn [[k _]]
                    (f k))))

(tests
  (split-map-by-key {1 :a 2 :b 3 :c} split-even-odd)
  := {:odd {1 :a, 3 :c}, :even {2 :b}})


(defn reduce-transient
  "Like reduce, makes the accumulator transient then persists the result."
  [f val coll]
  (persistent!
    (reduce f
            (transient val)
            coll)))


(defn select-keys! [m key-seq]
  (reduce-transient
    (fn [acc key]
      (conj! acc (find m key)))
    {}
    key-seq))

(tests
  (select-keys! {:a 1 :b 2 :c 3} [:a :c]) := {:a 1, :c 3})

