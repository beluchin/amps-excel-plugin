(ns simple-amps.functional.state
  (:refer-clojure :exclude [alias])
  (:require [functional :as f]))

(comment 
  ;; this is the shape of the state
  {;; user state
   :alias->sub ...
   :alias->qvns-set ... ;; the set is non-empty
   :id->alias+qvns ...
   ;; 

   ;; connection state
   :sub->activated-qvns-set ...

   :uri->executor ...

   :sub->ampsies ...
   :uri->client ...
   ;; 
   })

(defn activated-qvns-set [state sub]
  (get-in state [:sub->activated-qvns-set sub]))

(defn after-new-alias-qvns [state a qvns]
  (update-in state [:alias->qvns-set a] (fnil conj #{}) qvns))

(defn after-new-alias->qvns-set [state alias qvns-set]
  (assoc-in state [:alias->qvns-set alias] qvns-set))

(defn after-new-executor-if-absent [state uri executor]
  (f/assoc-in-if-missing state [:uri->executor uri] executor))

(defn after-new-id-alias+qvns [state id alias+qvns]
  (assoc-in state [:id->alias+qvns id] alias+qvns))

(defn after-new-alias->sub [state alias sub]
  (assoc-in state [:alias->sub alias] sub))

(defn after-new-sub-activated-qvns-set [state sub qvns-set]
  (assoc-in state [:sub->activated-qvns-set sub] qvns-set))

(defn after-new-sub-ampsies [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(declare after-remove)
(defn after-remove-alias->qvns [state alias]
  (after-remove state :alias->qvns-set [alias]))

(declare after-remove)
(defn after-remove-id [state id]
  (after-remove state :id->alias+qvns [id]))

(declare after-remove)
(defn after-remove-sub->ampsies [state coll]
  (after-remove state :sub->ampsies coll))

(declare after-remove)
(defn after-remove-sub->activated-qvns-set [state coll]
  (after-remove state :sub->activated-qvns-set coll))

(declare after-remove)
(defn after-remove-uri->client [state coll]
  (after-remove state :uri->client coll))

(declare after-remove)
(defn after-remove-uri->executor [state coll]
  (after-remove state :uri->executor coll))

(defn alias+qvns [state id]
  (get-in state [:id->alias+qvns id]))

(defn alias->qvns-set
  [state]
  (:alias->qvns-set state))

(defn alias->sub  [state]
  (:alias->sub state))

(defn ampsies 
  [state sub]
  (get-in state [:sub->ampsies sub]))

(defn client
  [state uri]
  (get-in state [:uri->client uri]))

(defn executor
  [state uri]
  (get-in state [:uri->executor uri]))

(defn qvns-set [state a]
  (get-in state [:alias->qvns-set a]))

(defn state-after-new-ampsies
  [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(defn state-after-new-client
  [state uri client]
  (assoc-in state [:uri->client uri] client))

(defn sub [state alias]
  (get-in state [:alias->sub alias]))

(defn sub->ampsies
  [state]
  (:sub->ampsies state))

(defn uri->client
  [state]
  (:uri->client state))

(defn- after-remove
  [state k coll]
  (when-let [v (k state)]
    (let [new-v (apply dissoc v coll)]
      (if (empty? new-v) (dissoc state k) (assoc state k new-v)))))
