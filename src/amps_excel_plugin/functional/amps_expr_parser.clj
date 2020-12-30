(ns amps-excel-plugin.functional.amps-expr-parser
  (:require [amps-excel-plugin.functional.expr :as expr]))

(declare keys-to-sequence index-fn)
(defn parse-subtree-selector
  "returns [ks index-fn] where ks are the keys to first sequential value
  on the subtree to select. index-fn is the index to the element to select
  out of the sequential value.

  The subtree represents a cross-section of m starting at the root.

  Right now only the = operator is supported."
  [m eq-expr]
  (let [ks (keys-to-sequence m eq-expr)]
    [ks (index-fn ks eq-expr)]))

(declare index-of-first)

(defn- index-fn 
  ^:toremove
  [ks expr]
  (fn [m-coll] (functional/index-of-first
                 #(expr/evaluate expr (assoc-in {} ks %))
                 m-coll)))

(defn- keys-to-sequence
  ^:toremove
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
