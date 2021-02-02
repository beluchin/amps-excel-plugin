(ns simple-amps.functional
  (:refer-clojure :exclude [filter name])
  (:require [simple-amps.functional.expr :as expr]
            [simple-amps.functional.state :as s]))

(defn ampsies
  "amps connectivity info"
  [client command-id sub-id]
  {:client client :command-id command-id :sub-id sub-id})

(defn combine
  [filter1 filter2 & filter-coll]
  (reduce #(format "%s AND (%s)" %1 %2)
          (format "(%s)" filter1)
          (conj filter-coll filter2) ))

(def error? keyword?)

(defn qvns-or-error 
  [filter context-expr value-expr consumer]
  {:filter (expr/parse-binary-expr filter)
   :context-expr (expr/parse-binary-expr context-expr)
   :value-expr (expr/parse-value-expr value-expr)
   :consumer consumer})

(declare subscribe-action+args)
(defn revisit
  [name state]
  (subscribe-action+args name state))

(defn subscribe-action+args 
  [name state]
  (let [sub (s/sub state name)
        coll (s/qvns-coll state name)
        filter (apply combine (:filter sub) (map :filter coll))]
    [:subscribe [sub filter]]))

(defn subscription
  [uri topic filter]
  (let [s {:uri uri :topic topic}]
    (if filter (assoc s :filter filter) s)))

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
      (first (clojure.core/filter #(expr/evaluate expr %) kites)))
    (when (expr/evaluate expr m) m)))

#_(defn leafpaths
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

(declare value)
#_(defn rtd+value-coll
  [m getValue-coll]
  (letfn [(message-matches [getValue] (expr/evaluate (:filter-expr getValue) m))
          (rtd+value [getValue] [(:rtd getValue)
                                 (value m
                                        (:context-expr getValue)
                                        (:value-expr getValue))])]
    (->> getValue-coll
         (filter message-matches)
         (map rtd+value))))

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

