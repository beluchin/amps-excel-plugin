(ns simple-amps.functional
  (:require [cheshire.core :as cheshire]
            [clojure.set :as set]
            [simple-amps.functional
             [expr  :as expr]
             [state :as s]]))

(defn ampsies
  "amps connectivity info"
  [client command-id sub-id]
  {:client client :command-id command-id :sub-id sub-id})

(defn combine
  [& filter-coll]
  (let [nil-less (remove nil? filter-coll)
        f1 (first nil-less)]
    (when f1
      (reduce #(format "%s AND (%s)" %1 %2)
              (format "(%s)" f1)
              (rest nil-less)))))

(def error? keyword?)

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

(declare in-scope? qvns-set value)
(defn handle
  "returns a collection of value+qvns so that they can be notified"
  [m sub state]
  (let [qvns-coll (qvns-set state sub)
        qvns-in-scope-coll (filter #(in-scope? m %) qvns-coll)]
    (map #(vector (value m %) %) qvns-in-scope-coll)))

(defn handle-json
  [json sub state]
  (handle (cheshire/parse-string json) sub state))

(defn in-scope?
  [m qvns]
  (expr/evaluate (second (:filter+expr qvns)) m))

(defn qvns-or-error 
  [fi context-expr value-expr consumer]
  {:filter+expr [fi (expr/parse-binary-expr fi)]
   :context-expr (expr/parse-binary-expr context-expr)
   :value-expr (expr/parse-value-expr value-expr)
   :consumer consumer})

(defmulti qvns-set #(cond (map? %2) :subscription :else :uri))
(defmethod qvns-set :subscription
  [state sub]
  (let [sub->alias (set/map-invert (s/alias->sub state))]
    (-> sub->alias
        (get sub)
        ((s/alias->qvns-set state)))))
(defmethod qvns-set :uri
  [state uri]
  (let [alias-coll (->> (s/alias->sub state)
                        (filter (comp #{uri} :uri second))
                        (map first))
        qvns-set-coll (->> alias-coll
                           (select-keys (s/alias->qvns-set state))
                           (map (comp vec second)))]
    (flatten qvns-set-coll)))

(defmulti state-after-delete #(cond (map? %2) :subscription
                                    (string? %2) :alias
                                    :else :amps-client))
(defmethod state-after-delete :amps-client
  [state client]
  (let [s (->> state
               s/uri->client 
               (filter (comp #{client} second))
               (s/state-after-delete-many state))]
    (->> s
         s/sub->ampsies
         (filter (comp #{client} :client second))
         (s/state-after-delete-many s))))

(defn subscribe-args 
  [sub qvns-set]
  [sub
   (apply combine (:filter sub) (map (comp first :filter+expr) qvns-set))
   qvns-set])

(defn subscription-action+args
  [a state]
  (let [sub (s/sub state a)
        qvns-set (s/qvns-set state a)]
    (when (and sub qvns-set)
      [:subscribe (subscribe-args sub qvns-set)])))

(defn subscription
  [uri topic fi]
  (let [s {:uri uri :topic topic}]
    (if fi (assoc s :filter fi) s)))

(declare evaluate)
(defn value
  ([m qvns] (value m (:context-expr qvns) (:value-expr qvns)))
  ([m context-expr value-expr] (-> m
                                   (first-kite context-expr)
                                   (evaluate value-expr))))

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

