(ns amps-excel-plugin.functional.amps-expr-parser
  (:require [amps-excel-plugin.functional.eq-expr :as eq-expr]
            [amps-excel-plugin.functional.expr :as expr]))

(declare keys-to-sequence index-fn)
(defn parse-subtree-selector
  "returns [ks index-fn] where ks are the keys to first sequential value
  on the subtree to select. index-fn is the index to the element to select
  out of the sequential value.

  Right now only the = operator is supported."
  [m eq-expr]
  (let [ks (keys-to-sequence m eq-expr)]
    [ks (index-fn ks eq-expr)]))

(declare index-of-first)

(defn- index-fn 
  [ks eq-expr]
  (let [suffix-ks (drop (count ks) (eq-expr/lhs eq-expr))
        rhs (eq-expr/rhs eq-expr)]
    (fn [m-coll] (functional/index-of-first #(= rhs (get-in % suffix-ks)) m-coll))))

(defn- keys-to-sequence
  [m boolean-expr]
  (letfn [(take-until-sequence
            [coll k]
            (let [result (conj coll k)]
              (if (sequential? (get-in m result))
                (reduced result)
                result)))]
    (reduce take-until-sequence [] (expr/common-path boolean-expr))))
