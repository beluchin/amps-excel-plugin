(ns simple-amps
  (:import [com.crankuptheamps.client
            Client
            Command
            MessageHandler
            ClientDisconnectHandler])  
  (:require [simple-amps.functional :as functional]))

(declare connect)
(defn ensure-connection-and-get-alias
  "Ensure the subscription is active. Returns the default alias.
  Supports client side filtering"
  ([uri topic]
   (connect uri topic)
   (functional/subscription-alias (functional/subscription uri topic)))

  ([uri topic s-filter]))

(defn get-value-sow-and-subscribe
  [s-alias s-msg-filter-expr s-context-expr s-value-expr])

(declare get-client get-new-client get-new-client-name uri->client)

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

(def ^:private uri->client (atom {}))


