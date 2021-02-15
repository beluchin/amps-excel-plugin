(ns simple-amps.functional.state
  (:require [clojure.set :as set]))

(comment 
  ;; this is the shape of the state
  {;; user defined
   :alias->sub ...
   :alias->qvns-set ...

   ;; implementation
   :uri->executor ...
   :alias->ampsies ...
   :uri->client ...})

(defn client
  [state uri]
  (get-in state [:uri->client uri]))

(defn executor
  [state uri]
  (get-in state [:uri->executor uri]))

(defmulti qvns-set #(cond (map? %2) :subscription
                          (string? %2) :alias
                          :else :amps-client))
(defmethod qvns-set :subscription
  [state sub]
  (let [sub->alias (set/map-invert (:alias->sub state))]
    (-> sub->alias
        (get sub)
        ((:alias->qvns-set state)))))
(defmethod qvns-set :alias
  [state a]
  (get-in state [:alias->qvns-set a]))
(defmethod qvns-set :amps-client
  [state amps-client]
  (let [client->uri (set/map-invert (:uri->client state))
        uri (get client->uri amps-client)
        sub-coll (filter #(= uri (:uri %)) (vals (:alias->sub state)))
        qvns-set--coll (map #(qvns-set state %) sub-coll)]
    (reduce set/join qvns-set--coll)))

(defn state-after-new-ampsies
  [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(defn state-after-new-alias
  [state a sub]
  (assoc-in state [:alias->sub a] sub))

(defn state-after-new-client-if-absent
  [state uri client]
  (update-in state [:uri->client uri] #(or % client)))

(defn state-after-new-executor-if-absent
  [state uri executor]
  (update-in state [:uri->executor uri] #(or % executor)))

(defn state-after-new-qvns
  [state a qvns]
  (update-in state [:alias->qvns-set a] (fnil conj #{}) qvns))

(defn sub
  [state a]
  (get-in state [:alias->sub a]))
