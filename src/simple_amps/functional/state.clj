(ns simple-amps.functional.state
  (:require [clojure.set :as set]))

(comment 
  ;; this is the shape of the state
  {;; user defined
   :alias->sub ...
   :alias->qvns-set ...

   ;; implementation
   :sub->activated-qvns-set ...
   :sub->ampsies ...
   :uri->executor ...
   :uri->client ...})

(defn activated-qvns-set
  [state sub]
  (get-in state [:sub->activated-qvns-set sub]))

(defn after
  [state sub ampsies activated-qvns-set]
  (-> state
      (assoc-in [:sub->ampsies sub] ampsies)
      (assoc-in [:sub->activated-qvns-set sub] activated-qvns-set)))

(declare after-delete)
(defn after-delete-sub->ampsies
  [state coll]
  (after-delete state :sub->ampsies coll))

(declare after-delete)
(defn after-delete-sub->activated-qvns-set
  [state coll]
  (after-delete state :sub->activated-qvns-set coll))

(declare after-delete)
(defn after-delete-uri->client
  [state coll]
  (after-delete state :uri->client coll))

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

(defn- after-delete
  [state k coll]
  (update state k #(apply dissoc % coll)))
