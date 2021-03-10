(ns simple-amps.functional
  (:require [cheshire.core :as cheshire]
            [clojure.set :as set]
            [simple-amps.functional
             [expr  :as expr]
             [state :as s]]
            [functional :as f]))

(defn ampsies
  "amps connectivity info"
  [client command-id sub-id]
  {:client client :command-id command-id :sub-id sub-id})

(defn dedup [and-filter or-filter-coll]
  (let [dedupped-or (set or-filter-coll)]
    [and-filter (remove #{and-filter} dedupped-or)]))

(declare combine-or)
(defn combine [and-filter or-filter-coll]
  (let [to-or (combine-or or-filter-coll)]
    (if and-filter
      (if to-or
        (format "(%s) AND (%s)" and-filter to-or)
        and-filter)
      to-or)))

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

(declare in-scope? qvns-set valuede)
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
  (let [expr (second (:filter+expr qvns))]
    (or (not expr) (expr/evaluate expr m))))

(defn qvns-or-error 
  [fi nested-context-expr value-expr consumer]
  (-> {:value-expr (expr/parse-value-expr value-expr)
       :consumer consumer}
      (#(if fi
          (assoc % :filter+expr [fi (expr/parse-binary-expr fi)])
          %))
      (#(if nested-context-expr
          (assoc %
                 :nested-context-expr
                 (expr/parse-binary-expr nested-context-expr))
          %))))

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

(defn resubscribe-args
  [sub qvns-super-set activated-qvns-set ampsies]
  [sub
   (apply combine (dedup (:filter sub) (map (comp first :filter+expr) qvns-super-set)))
   qvns-super-set
   (set/difference qvns-super-set activated-qvns-set)
   ampsies])

(defmulti state-after-delete #(cond (vector? %2) :alias+qvns
                                    (string? %2) :alias
                                    :else :amps-client))
(defmethod state-after-delete :amps-client
  [state client]
  (let [uri-coll (->> state
                      s/uri->client 
                      (filter (comp #{client} second))
                      (map first))
        sub-coll (->> state
                      s/sub->ampsies
                      (filter (comp #{client} :client second))
                      (map first))]
    (-> state 
        (s/after-delete-uri->client uri-coll)
        (s/after-delete-sub->ampsies sub-coll)
        (s/after-delete-sub->activated-qvns-set sub-coll))))

(defn subscribe-args 
  [sub qvns-set]
  [sub
   (apply combine (dedup (:filter sub) (map (comp first :filter+expr) qvns-set)))
   qvns-set])

(defn subscription-action+args
  [a state]
  (let [sub (s/sub state a)
        qvns-set (s/qvns-set state a)]
    (when (and sub qvns-set)
      (if-let [ampsies (s/ampsies state sub)]
        [:resubscribe (resubscribe-args sub
                                        qvns-set
                                        (s/activated-qvns-set state sub)
                                        ampsies)]
        [:subscribe (subscribe-args sub qvns-set)]))))

(defn subscription
  [uri topic fi]
  (let [s {:uri uri :topic topic}]
    (if fi (assoc s :filter fi) s)))

(declare evaluate)
(defn value
  ([m qvns] (value m (:nested-context-expr qvns) (:value-expr qvns)))
  ([m nested-context-expr value-expr]
   (if nested-context-expr
     (-> m
         (first-kite nested-context-expr)
         (evaluate value-expr))
     (evaluate m value-expr))))

(defn combine-or [filter-coll]
  (when (every? (comp not nil?) filter-coll)
    (let [ff (first filter-coll)]
      (if (< 1 (count filter-coll))
        (reduce #(format "%s OR (%s)" %1 %2)
                (format "(%s)" ff)
                (rest filter-coll))
        ff))))

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

