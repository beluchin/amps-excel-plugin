(ns amps-excel-plugin.functional.multi-message
  (:require [amps-excel-plugin.functional :as functional]
            [clojure.string :as string]))

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

(declare row-key rows-for-sequential-leaf)
(defn rows 
  [leafpath m1 m2]
  (let [leaf1 (get-in m1 leafpath)
        leaf2 (get-in m2 leafpath)]
    (if (some sequential? [leaf1 leaf2])
      (rows-for-sequential-leaf leafpath leaf1 leaf2)
      [[(row-key leafpath) leaf1 leaf2]])))

(defn side-by-side
  "a sequence of rows of a side-by-side rendering of two maps"
  [m1 m2]
  (mapcat #(rows % m1 m2)
          (merged (functional/leafpaths m1) (functional/leafpaths m2))))

(declare parent)

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

(defn- ok-advance-first?
  "only one of lp1 and lp2 can be nil"
  [prior-lp1 lp1 prior-lp2 lp2]
  (cond
    (nil? lp2) true
    (nil? lp1) false
    (ancestor? (parent prior-lp1) lp1) true
    :else (not (ancestor? (parent prior-lp2) lp2))))

(defn- parent
  "nil if key is nil or contains only one element"
  [lp]
  (butlast lp))

(defn- rows-for-sequential-leaf
  "at least on the leaves must be sequential"
  [leafpath leaf1 leaf2]
  (let [size (max (count leaf1) (count leaf2))]
    (letfn [(pad [sequential x] (concat sequential (repeat x)))]
      (mapcat (partial rows leafpath)
              (take size (pad (map #(assoc-in {} leafpath %) leaf1) nil))
              (take size (pad (map #(assoc-in {} leafpath %) leaf2) nil))))))

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
