(ns simple-amps.functional.state
  (:require [clojure.set :as set]))

;; this module represents a heterogeneous map 
;; with no relationships among the keys - the semantics among
;; keys are established elsewhere. 

(comment 
  ;; this is the shape of the state
  {;; user defined
   :alias->sub ...
   :alias->qvns-set ...
   :id->alias+qvns ...

   ;; implementation
   :sub->activated-qvns-set ...
   :sub->ampsies ...
   :uri->executor ...
   :uri->client ...})

(defn activated-qvns-set
  [state sub]
  (get-in state [:sub->activated-qvns-set sub]))

(defn after-new-alias-qvns [state a qvns]
  (update-in state[:alias->qvns-set a] (fnil conj #{}) qvns))

(defn after-new-id-alias+qvns [state id alias+qvns]
  (assoc-in state [:id->alias+qvns id] alias+qvns))

(defn after-new-sub-activated-qvns-set [state sub qvns-set]
  (assoc-in state [:sub->activated-qvns-set sub] qvns-set))

(defn after-new-sub-ampsies [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(declare after-remove)
(defn after-remove-sub->ampsies
  [state coll]
  (after-remove state :sub->ampsies coll))

(declare after-remove)
(defn after-remove-sub->activated-qvns-set
  [state coll]
  (after-remove state :sub->activated-qvns-set coll))

(declare after-remove)
(defn after-remove-uri->client
  [state coll]
  (after-remove state :uri->client coll))

(defn alias->qvns-set
  [state]
  (:alias->qvns-set state))

(defn alias->sub
  [state]
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

(defn qvns-set
  [state a]
  (get-in state [:alias->qvns-set a]))

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

(defn sub
  [state a]
  (get-in state [:alias->sub a]))

(defn sub->ampsies
  [state]
  (:sub->ampsies state))

(defn uri->client
  [state]
  (:uri->client state))

(defn- after-remove
  [state k coll]
  (update state k #(apply dissoc % coll)))
