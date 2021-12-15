(ns amps.content-filter
  (:require andor)
  (:import [andor Or]))

(defprotocol Remove
  (remove [cf to-remove]))
(extend-protocol Remove
  Object
  (remove [this to-remove]
    (when-not (= this to-remove)
      (throw (UnsupportedOperationException.)))))

(extend-type Or
  Remove
  (remove [this to-remove]
    (let [operand-set (:operand-set this)]
      (if (contains? operand-set to-remove)
        (apply andor/or (clojure.core/remove #{to-remove} operand-set))
        this))))

(def add andor/or)

