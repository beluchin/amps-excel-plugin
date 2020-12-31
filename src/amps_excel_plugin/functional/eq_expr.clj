(ns amps-excel-plugin.functional.eq-expr)

(declare lhs rhs)
(defn evaluate
  [expr m]
  (= (rhs expr) (get-in m (lhs expr))))

(def lhs first)

(def rhs second)
