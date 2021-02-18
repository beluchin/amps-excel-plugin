(ns simple-amps.functional
  (:refer-clojure :exclude [alias filter])
  (:require [simple-amps.functional.expr :as expr]
            [simple-amps.functional.state :as s]
            [cheshire.core :as cheshire]
            [simple-amps.functional.state :as f-state]))

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
      (first (clojure.core/filter #(expr/evaluate expr %) kites)))
    (when (expr/evaluate expr m) m)))

(declare in-scope? value)
(defn handle
  "returns a collection of value+qvns so that they can be notified"
  [m sub state]
  (let [qvns-coll (s/qvns-set state sub)
        qvns-in-scope-coll (clojure.core/filter #(in-scope? m %) qvns-coll)]
    (map #(vector (value m %) %) qvns-in-scope-coll)))

(defn handle-json
  [json sub state]
  (handle (cheshire/parse-string json) sub state))

(defn in-scope?
  [m qvns]
  (expr/evaluate (second (:filter+expr qvns)) m))

(defn qvns-or-error 
  [filter context-expr value-expr consumer]
  {:filter+expr [filter (expr/parse-binary-expr filter)]
   :context-expr (expr/parse-binary-expr context-expr)
   :value-expr (expr/parse-value-expr value-expr)
   :consumer consumer})

(declare subscribe-action+args)
(defn revisit
  [alias state]
  (cond
    (not (f-state/sub state alias)) nil
    (not (f-state/qvns-set state alias)) nil
    :else (subscribe-action+args alias state)))

(defmulti state-after-delete #(cond (map? %2) :subscription
                                    (string? %2) :alias
                                    :else :amps-client))
(defmethod state-after-delete :amps-client
  [state client]
  (let [s (->> state
               s/uri->client 
               (clojure.core/filter (comp #{client} second))
               (s/state-after-delete-many state))]
    (->> s
         s/sub->ampsies
         (clojure.core/filter (comp #{client} :client second))
         (s/state-after-delete-many s))))

(defn subscribe-action+args 
  [alias state]
  (let [sub (s/sub state alias)
        coll (s/qvns-set state alias)
        filter (apply combine (:filter sub) (map (comp first :filter+expr) coll))]
    [:subscribe [sub filter]]))

(defn subscription
  [uri topic filter]
  (let [s {:uri uri :topic topic}]
    (if filter (assoc s :filter filter) s)))

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

