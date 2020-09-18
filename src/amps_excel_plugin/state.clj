(ns amps-excel-plugin.state
  (:require [amps-excel-plugin.amps :as amps]))

(declare subscription-id->data subscription-id->subscription)

(defn get-data
  [subscription-id]
  (@subscription-id->data subscription-id))

(defn get-subscription 
  [subscription-id]
  (@subscription-id->subscription subscription-id))

(defn new-subscription-id
  [uri topic]
  (let [uri-components (amps/components uri)]
    (format "%s:%s@%s"
            (::amps/message-format uri-components)
            topic
            (::amps/host-port uri-components))))

(defn put-data
  [subscription-id data]
  (swap! subscription-id->data assoc subscription-id data))

(defn put-subscription
  [subscription-id subscription]
  (swap! subscription-id->subscription assoc subscription-id subscription))

(def ^:private subscription-id->data (atom {}))

(def ^:private subscription-id->subscription (atom {}))
