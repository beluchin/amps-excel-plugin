(ns andor
  (:refer-clojure :exclude [and or])
  (:require [clojure.set :as set]))

(defrecord And [operand-set])

(declare or try-to-recombine-or-into-and)
(defrecord Or [operand-set])

(declare and)
(defn- try-to-recombine-or-into-and [ands]
  (let [sets (map :operand-set ands)
        intersection (apply set/intersection sets)]
    (if (seq intersection)
      (->And (conj intersection
                   (->Or (set/difference (apply set/union sets) intersection))))
      (->Or ands))))

(declare or)
(defn add [cf1 cf2]
  (or cf1 cf2))

(defn and
  ([x] x)
  ([x y & more]
   (let [non-nil-args (set (filter some? (conj more x y)))]
     (when (seq non-nil-args)
       (if (= 1 (count non-nil-args))
         (first non-nil-args)
         (->And non-nil-args))))))

(defn optimize [expr]
  (if (clojure.core/and (instance? Or expr)
                        (every? #(instance? And %) (:operand-set expr)))
    (try-to-recombine-or-into-and (:operand-set expr))
    expr))

(defn or
  ([x] x)
  ([x y & more]
   (let [non-nil-args (set (filter some? (conj more x y)))]
     (when (seq non-nil-args)
       (if (= 1 (count non-nil-args))
         (first non-nil-args)
         (->Or non-nil-args))))))
