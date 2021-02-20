(ns simple-amps.functional.state
  (:require [clojure.set :as set]))

(comment 
  ;; this is the shape of the state
  {;; user defined
   :alias->sub ...
   :alias->qvns-set ...

   ;; implementation
   :uri->executor ...
   :sub->ampsies ...
   :uri->client ...})

(defn alias->qvns-set
  [state]
  (:alias->qvns-set state))

(defn alias->sub
  [state]
  (:alias->sub state))

(defn client
  [state uri]
  (get-in state [:uri->client uri]))

(defn executor
  [state uri]
  (get-in state [:uri->executor uri]))

(defn qvns-set
  [state a]
  (get-in state [:alias->qvns-set a]))

(defmulti state-after-delete-many
  #(cond (map? (ffirst %2))    :sub+ampsies
         (string? (ffirst %2)) :uri+client
         :else                 (throw (UnsupportedOperationException.))))
(defmethod state-after-delete-many :sub+ampsies
  [state coll]
  (reduce #(update %1 :sub->ampsies dissoc (first %2)) state coll))
(defmethod state-after-delete-many :uri+client
  [state coll]
  (reduce #(update %1 :uri->client dissoc (first %2)) state coll))

(defn state-after-new-ampsies
  [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(defn state-after-new-alias
  [state a sub]
  (assoc-in state [:alias->sub a] sub))

(defn state-after-new-client
  [state uri client]
  (assoc-in state [:uri->client uri] client))

(defn state-after-new-executor-if-absent
  [state uri executor]
  (update-in state [:uri->executor uri] #(or % executor)))

(defn state-after-new-qvns
  [state a qvns]
  (update-in state [:alias->qvns-set a] (fnil conj #{}) qvns))

(defn sub
  [state a]
  (get-in state [:alias->sub a]))

(defn sub->ampsies
  [state]
  (:sub->ampsies state))

(defn uri->client
  [state]
  (:uri->client state))
