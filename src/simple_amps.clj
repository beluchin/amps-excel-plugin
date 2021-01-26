(ns simple-amps
  (:import [com.crankuptheamps.client
            Client
            Command
            MessageHandler
            ClientDisconnectHandler])  
  (:require [simple-amps.functional :as f])
  (:refer-clojure :exclude [alias filter name]))

(declare on-aliased state)
(defn alias
  "Returns nil.

  No validation of uri occurs here. If the uri is malformed it will
  be notified via the query-and-subscribe calls. 

  The connection to AMPS will take place only after a query-and-subscribe 
  references the alias"
  ([name ^String uri ^String topic] (alias name uri topic nil))

  ([name ^String uri ^String topic ^String filter]
   (let [stream (f/stream uri topic filter)
         [old-state] (swap-vals! state f/state-with-alias name stream)]
     (on-aliased name stream old-state))))

(defprotocol QueryValueAndSubscribeConsumer
  (on-value [this x])
  (on-active-no-sow [this])

  ;; reason: invalid uri | cannot connect | connection was closed
  (on-inactive [this reason])

  (on-unaliased [this]))

(declare on-query-value-and-subscribe)
(defn query-value-and-subscribe
  "Returns nil or an error when the args are malformed"
  [name ^String filter ^String context-expr ^String value-expr consumer]
  (let [qvns-or-error (f/qvns-or-error
                        filter context-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (let [qvns qvns-or-error
            [old-state] (swap-vals!
                          state f/state-with-qvns name qvns)]
        (on-query-value-and-subscribe
          name qvns old-state)))))

#_(defn unsubscribe
  [qns-id]
  (throw (UnsupportedOperationException.)))

(declare assoc-if-absent get-client get-new-client get-new-client-name new-lock
         uri->client)

(defn- connect
  [uri topic]
  (let [client     (get-client uri)
        command    (.. (Command. "subscribe") (setTopic topic))
        handler    (reify MessageHandler
                     (invoke [_ msg] ((constantly :no-op) (.getData msg))))
        command-id (.executeAsync client command handler)]
    [client command-id]))

(defn- get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (let [u->c (swap! uri->client #(if-not (% uri)
                                   (assoc % uri (get-new-client uri))
                                   %))]
    (u->c uri)))

(defn- get-lock
  [subscription]
  #_(swap! subscription->lock assoc-if-absent subscription new-lock))

(defn- get-new-client
  [uri]
  (doto (Client. (get-new-client-name))
    (.connect uri)
    (.setDisconnectHandler (reify ClientDisconnectHandler
                             (invoke [_ client]
                               (logging/info "client disconnected"))))
    (.logon)))

(defn- get-new-client-name
  []
  (format "%s:amps-excel-plugin:%s"
          (System/getProperty "user.name")
          (.toString (java.util.UUID/randomUUID))))

(defn- on-aliased
  [name stream old-state]
  )

(defn- subscribe-and-get-client+command-id
  [uri topic getData-consumer]
  (let [client     (get-client uri)
        command    (.. (Command. "subscribe") (setTopic topic))
        handler    (reify MessageHandler
                     (invoke [_ msg] (getData-consumer (.getData msg))))
        command-id (.executeAsync client command handler)]
    [client command-id]))

(defn- unsubscribe
  [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom f/EmptyState))

(def ^:private uri->client (atom {}))


