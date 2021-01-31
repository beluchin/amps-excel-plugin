(ns simple-amps
  (:import [com.crankuptheamps.client
            Client
            Command
            MessageHandler
            ClientDisconnectHandler])  
  (:require [simple-amps.functional :as f])
  (:refer-clojure :exclude [alias filter name]))

(declare on-aliased save-alias)
(defn alias
  "Returns nil.

  The connection to AMPS will take place only after a query-value-and-subscribe 
  references the alias.

  No validation of uri occurs here. If the uri is malformed it will
  be notified via the query-value-and-subscribe calls."
  ([name ^String uri ^String topic] (alias name uri topic nil))

  ([name ^String uri ^String topic ^String filter]
   (let [sub (f/subscription uri topic filter)]
      (save-alias name sub)
      (on-aliased name sub))))

(defprotocol QueryValueAndSubscribeConsumer
  (on-value [this x])
  (on-active-no-sow [this])

  ;; reason: invalid uri | cannot connect | connection was closed
  (on-inactive [this reason])

  (on-unaliased [this]))

(declare on-query-value-and-subscribe save-qvns)
(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [name ^String filter ^String context-expr ^String value-expr consumer]
  (let [qvns-or-error (f/qvns-or-error filter context-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (do (save-qvns name qvns-or-error)
          (on-query-value-and-subscribe name)))))

#_(defn unsubscribe
  [qns-id]
  (throw (UnsupportedOperationException.)))

(declare async get-client get-new-client get-new-client-name get-subscription
         revisit state uri->client)

(defn- connect
  [uri topic]
  (let [client     (get-client uri)
        command    (.. (Command. "subscribe") (setTopic topic))
        handler    (reify MessageHandler
                     (invoke [_ msg] ((constantly :no-op) (.getData msg))))
        command-id (.executeAsync client command handler)]
    [client command-id]))

(defn function
  [kw]
  (resolve (symbol (name kw))))

(defn- get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (let [u->c (swap! uri->client #(if-not (% uri)
                                   (assoc % uri (get-new-client uri))
                                   %))]
    (u->c uri)))

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
  [name sub]
  )

(defn- on-query-value-and-subscribe
  [name]
  (let [sub (get-subscription name)]
    
    ;; no blocking calls on the thread where the excel functions are called.
    (async (uri sub) revisit name)))

(defn- revisit
  "subscribes | replaces filter | unsubscribes - depending on the state

  Assumes no concurrency by subscription"
  [name]
  (let [[action-kw args] (f/revisit name @state)]
    (apply (function action-kw) args)))

(defn- save-alias
  [name sub]
  (first (swap-vals! state f/state-after-new-alias name sub)))

(defn- save-qvns
  [name qvns]
  (first (swap-vals! state f/state-after-new-qvns name qvns)))

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
