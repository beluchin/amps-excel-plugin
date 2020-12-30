(ns amps-excel-plugin.functional
  (:require [amps-excel-plugin.functional.expr :as expr]))

(declare keys-to-first-sequence)
(defn kite 
  "Returns a map extracted from m based on the expr. The result is a sequence of 
  nested one-key maps starting from the root and ending on a complete map value 
  taken from a map sequence value on the original map"
  [m expr]
  (let [ks    (keys-to-first-sequence m expr)
        coll  (get-in m ks)
        kites (map #(assoc-in {} ks %) coll)]
    (first (filter #(expr/evaluate expr %) kites))))

(defn leafpaths
  ;; https://stackoverflow.com/a/21769786/614800
  "a leafpath is a sequence of keys from the top to a value that is not a map"
  [m]
  (if (map? m)
    (vec 
     (mapcat (fn [[k v]]
               (let [sub (leafpaths v)
                     nested (map #(into [k] %) (filter (comp not empty?) sub))]
                 (if (seq nested)
                   nested
                   [[k]])))
             m))
    []))

(defn value-in
  "on m, the ks1 lead to a sequence of maps. A map from the sequence
  is selected using the index-fn and from there the ks2 are used
  to get to a value. The index-fn takes a sequence and returns an index"
  ^:deprecated ^:toremove
  [m ks1 index-fn ks2]
  (-> m
      (get-in ks1)
      (#(nth % (index-fn %)))
      (get-in ks2)))

(defn- keys-to-first-sequence
  [m expr]
  (letfn [(take-until-sequence
            [coll k]
            (let [result (conj coll k)]
              (if (sequential? (get-in m result))
                (reduced result)
                result)))]
    (let [result (reduce take-until-sequence [] (expr/common-path expr))]
      (if (sequential? (get-in m result))
        result
        (throw (IllegalArgumentException. "expression does not reference a sequential value"))))))
