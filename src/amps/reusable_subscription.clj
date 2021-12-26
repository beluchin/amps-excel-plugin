(ns amps.reusable-subscription
  (:refer-clojure :exclude [take])
  (:require [amps.reusable-subscription.internal :as internal]
            helpers)
  (:import [amps.reusable_subscription.internal Subscribe]))

;; sow and subscribe, with oof

(def ^:private client-to-executor (atom nil))
(def ^:private resubs (atom nil))

(defprotocol ^:private Take
  (take [action client topic filter msg-handler]))
(declare execute-async executor get-unique-including message-handler submit)
(extend-protocol Take
  Subscribe
  (take [action client topic filter msg-consumer]
    (let [sub-id (get-unique-including topic)
          executor (executor client)]
      [sub-id (execute-async client
                             topic
                             sub-id
                             filter
                             (message-handler
                               #(->> %
                                     .copy
                                     (submit executor msg-consumer))))])))

(defn- execute-async [client topic sub-id filter msg-handler]
  (.executeAsync client
                 (.. (com.crankuptheamps.client.Command. "sow_and_subscribe")
                     (setTopic topic)
                     (setSubId sub-id)
                     (setFilter filter)
                     (setOptions "oof"))
                 msg-handler))

(defn- executor [client]
  (let [e (get @client-to-executor client)]
    (or e
        (let [new-e (java.util.concurrent.Executors/newSingleThreadExecutor)]
          (swap! client-to-executor helpers/assoc-if-missing client new-e)
          (get @client-to-executor client)))))

(declare get-unique-string)
(defn- get-unique-including [& strs]
  (clojure.string/join "-" (flatten [strs (get-unique-string)])))

(defn- get-unique-string []
  (java.util.UUID/randomUUID))

(defn- message-handler [msg-consumer]
  (reify com.crankuptheamps.client.MessageHandler
    (invoke [_ msg] (msg-consumer msg))))

(defn- submit [executor msg-consumer msg]
  (.submit executor #(msg-consumer msg)))

(defn sow-and-subscribe

  "with oof

  msg-consumer: (fn [msg] ...)
  sow-completed-runner: (fn [] ...)

  the msg consumer is called in a thread-safe manner i.e. no
  concurrency per client/topic"

  ([client topic filter msg-consumer]
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
                     (take @action client topic filter msg-consumer))
              nil)
          (catch Exception ex
            (swap! resubs internal/failed-to-subscribe client topic)
            (throw ex)))))
  
  ([client topic filter msg-consumer sow-completed-runnable]
   (throw (UnsupportedOperationException.))))

(defn unsubscribe
  ([client topic] (throw (UnsupportedOperationException.)))
  ([client topic filter]))

(defn disconnected [client])
