(ns amps-excel-plugin.state
  (:refer-clojure :exclude [dissoc find])
  (:require [amps-excel-plugin.amps :as amps]
            [amps-excel-plugin.logging :as logging]))

(declare id->data-subscription)

(defn assoc-data-if-subscribed
  [id data]
  (swap! id->data-subscription 
         #(let [m? (clojure.core/find % id)]
            (if m?
              (assoc-in % [id :data] data)
              %))))

(defn assoc-subscription
  [id subscription]
  (swap! id->data-subscription assoc-in [id :subscription] subscription))

(defn dissoc
  [id]
  (swap! id->data-subscription clojure.core/dissoc id))

(declare try-get-subscription)
(defn get-subscription 
  [id]
  (let [subscription? (try-get-subscription id)]
    (if subscription?
      subscription?
      (throw (IllegalStateException.)))))

(defn subscription? [s] (contains? @id->data-subscription s))

(defn try-get [s] (get @id->data-subscription s))

(defn try-get-subscription [s] (get-in @id->data-subscription [s :subscription]))


(def ^:private id->data-subscription (atom {}))
