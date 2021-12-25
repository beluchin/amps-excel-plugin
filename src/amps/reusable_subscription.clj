(ns amps.reusable-subscription
  (:refer-clojure :exclude [take])
  (:require [amps.reusable-subscription.internal :as internal])
  (:import [amps.reusable_subscription.internal Subscribe]
           [com.crankuptheamps.client
            Client
            ClientDisconnectHandler
            Command
            Message$Command
            MessageHandler]
           [com.crankuptheamps.client.exception
            CommandException
            ConnectionException]))

(def ^:private resubs (atom nil))

(defprotocol ^:private Take
  (take [action client topic filter msg-handler]))
(declare get-unique-including)
(extend-protocol Take
  Subscribe
  (take [action client topic filter msg-handler]
    (let [sub-id (get-unique-including topic)]
      [sub-id (.executeAsync client
                             (.. (Command. "sow_and_subscribe")
                                 (setTopic topic)
                                 (setSubId sub-id)
                                 (setFilter filter)
                                 (setOptions "oof"))
                             msg-handler)])))

(declare get-unique-string)
(defn- get-unique-including [& strs]
  (clojure.string/join "-" (flatten [strs (get-unique-string)])))

(defn- get-unique-string []
  (java.util.UUID/randomUUID))

(defn sow-and-subscribe
    "with oof"
    ([client topic filter msg-handler]
     (let [action (volatile! nil)]
       (swap! resubs
              (fn [resubs] (let [r (internal/ensure resubs client topic filter)]
                             (vreset! action (internal/action r))
                             (internal/resubs r))))
       (try (do (apply swap!
                       resubs
                       internal/subscribed
                       client
                       topic
                       (take @action client topic filter msg-handler))
                nil)
            (catch Exception ex
              (swap! resubs internal/failed-to-subscribe client topic)
              (throw ex)))))
  
    ([client topic filter msg-handler sow-completed-handler]
     (throw (UnsupportedOperationException.))))

(defn unsubscribe
  ([client topic] (throw (UnsupportedOperationException.)))
  ([client topic filter]))

(defn disconnected [client])
