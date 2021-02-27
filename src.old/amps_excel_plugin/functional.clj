(ns amps-excel-plugin.functional
  (:require [amps-excel-plugin.amps.functional :as amps.functional]
            [amps-excel-plugin.functional.expr :as expr]))

(declare keys-to-first-coll)
(defn first-kite 
  "Returns a map extracted from m based on the expr or nil. 

  When a map is returned, it is either m itself or a series of nested one-key 
  maps starting from the root and ending on a complete map value taken from a 
  map collection value on m.

  Nested collections are not supported."
  [m expr]
  (if-let [ks (keys-to-first-coll m expr)]
    (let [coll (get-in m ks)
          kites (map #(assoc-in {} ks %) coll) ]
      (first (filter #(expr/evaluate expr %) kites)))
    (when (expr/evaluate expr m) m)))

(defn subscription-alias
  [subscription]
  (format "%s:%s@%s"
          (:message-type subscription)
          (:topic subscription)
          (:host-port subscription)))

(declare value)
(defn rtd+value-coll
  [m getValue-coll]
  (letfn [(message-matches [getValue] (expr/evaluate (:filter-expr getValue) m))
          (rtd+value [getValue] [(:rtd getValue)
                                 (value m
                                        (:context-expr getValue)
                                        (:value-expr getValue))])]
    (->> getValue-coll
         (filter message-matches)
         (map rtd+value))))

(defn subscription
  [uri topic]
  (assoc (amps.functional/components uri) :topic topic))

(declare evaluate)
(defn value
  [m context-expr value-expr]
  (-> m
      (first-kite context-expr)
      (evaluate value-expr)))

(defn- evaluate
  [m value-expr]
  (expr/evaluate value-expr m))

(defn- keys-to-first-coll
  [m expr]
  (letfn [(take-until-coll
            [coll k]
            (let [result (conj coll k)]
              (if (sequential? (get-in m result))
                (reduced result)
                result)))]
    (let [result (reduce take-until-coll [] (expr/common-path expr))]
      (when (sequential? (get-in m result)) result))))

