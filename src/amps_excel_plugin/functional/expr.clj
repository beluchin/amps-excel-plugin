(ns amps-excel-plugin.functional.expr
  (:require [amps-excel-plugin.functional.eq-expr :as eq-expr]))

(defn common-path
  [expr]
  (first expr))

(defn evaluate
  [expr m]
  ;; only eq-expressions supported at this time.
  (eq-expr/evaluate expr m))
