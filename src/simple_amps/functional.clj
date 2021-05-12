(ns simple-amps.functional
  (:refer-clojure :exclude [alias])
  (:require [cheshire.core :as cheshire]
            [clojure.set :as set]
            [simple-amps.functional
             [expr  :as expr]
             [state :as s]]
            [functional :as f]))

(declare subscribe-args resubscribe-args)
(defn actions 
  ([alias state]
   (let [sub                 (s/sub state alias)
         qvns-coll           (s/qvns-set state alias)
         ampsies             (s/ampsies state sub)
         activated-qvns-coll (s/activated-qvns-set state sub)]
     (actions sub qvns-coll ampsies activated-qvns-coll)))
  ([sub qvns-coll ampsies activated-qvns-coll]
   (when (and sub qvns-coll)
     (if ampsies
       [:resubscribe (resubscribe-args sub
                                       qvns-coll
                                       activated-qvns-coll
                                       ampsies)]
       [:subscribe (subscribe-args sub qvns-coll)]))))

(defn aliases [uri state]
  (->> state
      s/alias->sub
      (filter (fn [[_ sub]] (= uri (:uri sub))))
      (map first)))

(defn ampsies [client command-id sub-id]
  {:client client :command-id command-id :sub-id sub-id})

(defn dedup [and-filter or-filter-coll]
  [and-filter (set (remove #{and-filter} or-filter-coll))])

(defn client-to-close+state [state alias uri]
  (when-not (seq (s/qvns-set state alias))
    (when-let [client (s/client state uri)]
      [client (-> state
                  (s/after-remove-uri->client [uri])
                  (s/after-remove-sub->ampsies [(s/sub state alias)]))])))

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
(defn first-nested-map 
  "Returns a map extracted from m based on the expr or nil. 

  When a map is returned, it is either m itself or a series of nested one-key 
  maps starting from the root and ending on a complete map value taken from a 
  map collection value on m.

  Nested collections are not supported."
  [m expr]
  (if-let [ks (keys-to-first-coll m expr)]
    (let [coll (get-in m ks)
          nested-map-coll (map #(assoc-in {} ks %) coll) ]
      (first (filter #(expr/evaluate expr %) nested-map-coll)))
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
  (let [expr (second (:filter+expr qvns))]
    (or (not expr) (expr/evaluate expr m))))

(defn qvns-or-error 
  [fi nested-map-expr value-expr consumer]
  (-> {:value-expr (expr/parse-value-expr value-expr)
       :consumer consumer}
      (#(if fi
          (assoc % :filter+expr [fi (expr/parse-binary-expr fi)])
          %))
      (#(if nested-map-expr
          (assoc %
                 :nested-map-expr
                 (expr/parse-binary-expr nested-map-expr))
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

(defn resubscribe-args [sub qvns-super-set activated-qvns-set ampsies]
  [sub
   (apply combine (dedup (:filter sub) (map (comp first :filter+expr) qvns-super-set)))
   qvns-super-set
   (set/difference qvns-super-set activated-qvns-set)
   ampsies])

(defn state-after-remove-client [state client]
  (let [uri-coll (->> state
                      s/uri->client 
                      (filter (comp #{client} second))
                      (map first))
        sub-coll (->> state
                      s/sub->ampsies
                      (filter (comp #{client} :client second))
                      (map first))]
    (-> state 
        (s/after-remove-uri->client uri-coll)
        (s/after-remove-sub->ampsies sub-coll)
        (s/after-remove-sub->activated-qvns-set sub-coll))))

(declare state-after-remove-activated-qvns state-after-remove-qvns)
(defn state-after-remove-qvns-call-id [state id]
  (let [[alias qvns] (s/alias+qvns state id)
        sub (s/sub state alias)]
    (-> state
        (s/after-remove-id id)
        (state-after-remove-activated-qvns sub qvns)
        (state-after-remove-qvns alias qvns))))

(defn subscribe-args [sub qvns-set]
  [sub
   (apply combine (dedup (:filter sub) (map (comp first :filter+expr) qvns-set)))
   qvns-set])

(defn subscription [uri topic fi]
  (let [s {:uri uri :topic topic}]
    (if fi (assoc s :filter fi) s)))

(defn uri [state alias]
  (:uri (s/sub state alias)))

(declare evaluate)
(defn value
  ([m qvns] (value m (:nested-map-expr qvns) (:value-expr qvns)))
  ([m nested-map-expr value-expr]
   (if nested-map-expr
     (-> m
         (first-nested-map nested-map-expr)
         (evaluate value-expr))
     (evaluate m value-expr))))

(defn- combine-or [filter-coll]
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

(defn- state-after-remove-activated-qvns [state sub qvns]
  (if-let [qvns-set (s/activated-qvns-set state sub)]
    (let [activated-qvns-set (disj qvns-set qvns)]
      (if (empty? activated-qvns-set)
        (s/after-remove-sub->activated-qvns-set state [sub])
        (s/after-new-sub-activated-qvns-set state sub activated-qvns-set)))
    state))

(defn- state-after-remove-qvns [state alias qvns]
  (if-let [qvns-set (s/qvns-set state alias)]
    (let [new-qvns-set (disj qvns-set qvns)]
      (if (empty? new-qvns-set)
        (s/after-remove-alias->qvns state alias)
        (s/after-new-alias-qvns state alias new-qvns-set)))
    state))
