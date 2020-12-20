(ns amps-excel-plugin.functional.multi-message
  (:require [amps-excel-plugin.functional :as functional]
            [clojure.string :as string]))

(declare leaf?)
(defn leaf [x] (when (leaf? x) x))

(def leaf? (complement map?))

(declare ok-advance-first?)
(defn merged
  "merges two sequences of leafpaths"
  [leafpaths1 leafpaths2]
  (loop [prior-lp1  nil
         lps1       leafpaths1
         prior-lp2  nil
         lps2       leafpaths2
         accum      []]
    (if (every? empty? [lps1 lps2])
      accum
      (let [lp1 (first lps1)
            lp2 (first lps2)]
        (cond
          (= lp1 lp2)
          (recur lp1 (next lps1) lp2 (next lps2) (conj accum lp1))
          
          (ok-advance-first? prior-lp1 lp1 prior-lp2 lp2)
          (recur lp1 (next lps1) prior-lp2 lps2 (conj accum lp1))
          
          :else ;; advance second
          (recur prior-lp1 lps1 lp2 (next lps2) (conj accum lp2)))))))

(declare row-key rows-for-sequential-leaf row-for-primitives)
(defn rows
  "assumes at least one of the values is a leaf"
  [leafpath m1 m2]
  (let [x1 (get-in m1 leafpath)
        x2 (get-in m2 leafpath)]
    (cond
      (some sequential? [x1 x2]) (rows-for-sequential-leaf leafpath x1 x2)
      (not (every? leaf? [x1 x2])) (row-for-primitives leafpath (leaf x1) (leaf x2))
      :else (row-for-primitives leafpath x1 x2))))

(defn side-by-side
  "a sequence of rows of a side-by-side rendering of two maps"
  [m1 m2]
  (mapcat #(rows % m1 m2)
          (merged (functional/leafpaths m1) (functional/leafpaths m2))))

(declare coll pad parent)

(defn- ancestor?
  [coll? k]
  (cond
    (nil? coll?)
    false
    
    (< (count k) (count coll?))
    false

    (every? #(= (first %) (second %)) (map vector coll? k))
    true
    
    :else
    false))

(defn- coll [x] (if (sequential? x) x [x]))

(defn- ok-advance-first?
  "only one of lp1 and lp2 can be nil"
  [prior-lp1 lp1 prior-lp2 lp2]
  (cond
    (nil? lp2) true
    (nil? lp1) false
    (ancestor? (parent prior-lp1) lp1) true
    :else (not (ancestor? (parent prior-lp2) lp2))))

(defn- pad [sequential x] (concat sequential (repeat x)))

(defn- parent
  "nil if key is nil or contains only one element"
  [lp]
  (butlast lp))

(defn- row-for-primitives
  [leafpath x1 x2]
  [[(row-key leafpath) x1 x2]])

(defn- rows-for-sequential-leaf
  "at least one leaf must be sequential - otherwise undefined behavior"
  [leafpath leaf1 leaf2]
  (let [coll1 (coll leaf1)
        coll2 (coll leaf2)
        size (max (count coll1) (count coll2))]
    (mapcat side-by-side 
            (take size (pad (map #(assoc-in {} leafpath %) coll1) nil))
            (take size (pad (map #(assoc-in {} leafpath %) coll2) nil)))))

(defn- row-key
  [strings]
  (string/join "/" (cons "" strings)))

(comment
  (conj [1 2] 3) ; [1 2 3]
  (rest []) ; ()
  (every? empty? [[] []])
  (vec (butlast [1 2]))
  (map vector [1 2] [3])
  (ancestor? '() [:a])
  (parent [])
  (butlast [1])
  (butlast nil)
  )
