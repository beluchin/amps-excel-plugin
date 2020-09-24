(ns amps-excel-plugin.state
  (:require [amps-excel-plugin.amps :as amps])
  (:refer-clojure :exclude [remove]))

(declare id->data-subscription)

(defn assoc-data-if-subscribed
  [id data]
  (swap! id->data-subscription 
         #(if (% id)
            (assoc-in % [id ::data] data)
            %)))

(defn assoc-subscription
  [id subscription]
  (swap! id->data-subscription assoc-in [id ::subscription] subscription))

(defn find-data [s] (get-in @id->data-subscription [s ::data]))

(defn find-subscription [s] (get-in @id->data-subscription [s ::subscription]))

(defn get-subscription 
  [id]
  (let [subscription? (find-subscription id)]
    (if subscription?
      subscription?
      (throw (IllegalStateException.)))))

(defn new-subscription-id
  [uri topic]
  (let [uri-components (amps/components uri)]
    (format "%s:%s@%s"
            (::amps/message-format uri-components)
            topic
            (::amps/host-port uri-components))))

(defn remove
  [id]
  (swap! id->data-subscription dissoc id))

(defn subscription? [s] (contains? @id->data-subscription s))

(def ^:private id->data-subscription (atom {}))
