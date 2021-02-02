(ns simple-amps
  (:refer-clojure :exclude [alias filter name])
  (:require [simple-amps.functional :as f]
            [simple-amps.functional.state :as f-state])
  (:import [com.crankuptheamps.client Client ClientDisconnectHandler Command MessageHandler]))

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

(declare get-executor)
(defn- asynch
  [uri f & args]
  (.submit (get-executor uri) #(apply f args)))

(defn- function
  [kw]
  (resolve (symbol (clojure.core/name kw))))

(declare get-new-client uri->client)
(defn- get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (let [u->c (swap! uri->client #(if-not (% uri)
                                   (assoc % uri (get-new-client uri))
                                   %))]
    (u->c uri)))

(declare save-executor-if-absent)
(defn- get-executor
  [uri]
  (let [e (f-state/executor state uri)]
    (if e
      e
      (let [new-e (java.util.concurrent.Executors/newSingleThreadExecutor)]
        (save-executor-if-absent uri new-e)
        (f-state/executor state uri)))))

(declare get-new-client-name)
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

(declare async get-subscription revisit)
(defn- on-query-value-and-subscribe
  [name]
  (let [sub (get-subscription name)]
    
    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) revisit name)))

(declare state)
(defn- revisit
  "subscribes | replaces filter | unsubscribes - depending on the state

  Assumes no concurrency by subscription"
  [name]
  (let [[action-kw args] (f/revisit name @state)]
    (apply (function action-kw) args)))

(defn- save-ampsies
  [sub ampsies]
  (swap! state f-state/state-after-new-ampsies sub ampsies))

(defn- save-alias
  [name sub]
  (swap! state f-state/state-after-new-alias name sub))

(defn- save-executor-if-absent
  [uri executor]
  (swap! state f-state/state-after-new-executor-if-absent uri executor))

(defn- save-qvns
  [name qvns]
  (swap! state f-state/state-after-new-qvns name qvns))

(declare uniq-id)
(defn- subscribe
  [sub filter]
  (let [client     (get-client (:uri sub))
        sub-id     (uniq-id)
        command    (.. (Command. "sow_and_subscribe")
                       (setTopic (:topic sub))
                       (setSubId sub-id)
                       (setFilter filter))
        handler    (reify MessageHandler
                     (invoke [_ msg] (.getData msg)))
        command-id (.executeAsync client command handler)]
    (save-ampsies sub (f/ampsies client command-id sub-id))))

(defn- uniq-id [] (str (java.util.UUID/randomUUID)))

(defn- unsubscribe
  [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom nil))

(def ^:private uri->client (atom nil))
