(ns amps.expr
  (:require andor)
  (:import [andor And Or]))

(defprotocol StringForm
  (string-form [expr] "the string that can be used on a subscription as the content filter"))
(extend-protocol StringForm
  String
  (string-form [expr] expr))

(extend-type And
  StringForm
  (string-form [expr] (->> (:operand-set expr)
                           (map #(format "(%s)" (string-form %)))
                           (String/join " AND "))))

(extend-type Or
  StringForm
  (string-form [expr] (->> (:operand-set expr)
                           (map #(format "(%s)" (string-form %)))
                           (String/join " OR "))))
