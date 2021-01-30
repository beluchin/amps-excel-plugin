(ns simple-amps
  (:import [com.crankuptheamps.client
            Client
            Command
            MessageHandler
            ClientDisconnectHandler])  
  (:require [simple-amps.functional :as f])
  (:refer-clojure :exclude [alias filter name]))

(declare on-aliased save)
(defn alias
  "Returns nil.

  The connection to AMPS will take place only after a query-and-subscribe 
  references the alias.

  No validation of uri occurs here. If the uri is malformed it will
  be notified via the query-and-subscribe calls. "
  ([name ^String uri ^String topic] (alias name uri topic nil))

  ([name ^String uri ^String topic ^String filter]
   (let [sub (f/subscription uri topic filter)
         old-state (save name sub)]
     (on-aliased name sub old-state))))

(defprotocol QueryValueAndSubscribeConsumer
  (on-value [this x])
  (on-active-no-sow [this])

  ;; reason: invalid uri | cannot connect | connection was closed
  (on-inactive [this reason])

  (on-unaliased [this]))

(declare on-query-value-and-subscribe)
(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [name ^String filter ^String context-expr ^String value-expr consumer]
  (let [qvns-or-error (f/qvns-or-error filter context-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (let [qvns qvns-or-error
            [old-state] (swap-vals! state f/state-after-new-qvns name qvns)]
        (on-query-value-and-subscribe name qvns old-state)))))

#_(defn unsubscribe
  [qns-id]
  (throw (UnsupportedOperationException.)))

(declare assoc-if-absent async connect-or-replace-filter get-client
         get-new-client get-new-client-name get-subscription new-lock uri
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

(defn- on-query-value-and-subscribe
  [name qvns old-state]
  (let [sub (get-subscription name)]
    (swap-vals! state f/state-after-new-qvns-filter sub (:filter qvns))

    ;; no blocking calls on the thread where the excel functions are called.
    (async (uri sub) connect-or-replace-filter sub)))

(defn- save
  [name sub]
  (first (swap-vals! state f/state-after-new-alias name sub)))

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

(def ^:private state (atom nil))

(def ^:private uri->client (atom {}))


