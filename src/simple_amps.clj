(ns simple-amps
  (:refer-clojure :exclude [alias filter])
  (:require [simple-amps.functional :as f]
            [simple-amps.functional.state :as f-state])
  (:import [com.crankuptheamps.client Client ClientDisconnectHandler Command
            Message$Command MessageHandler]))

(declare on-aliased save-alias)
(defn alias
  "Returns any other aliases associated with the same subscription or nil.

  The connection to AMPS will take place only after a query-value-and-subscribe 
  references the alias.

  No validation of uri occurs here. If the uri is malformed it will
  be notified via the query-value-and-subscribe calls."
  ([^String s ^String uri ^String topic] (alias s uri topic nil))

  ([^String s ^String uri ^String topic ^String filter]
   (let [sub (f/subscription uri topic filter)]
      (save-alias s sub)
      (on-aliased s sub))))

(defprotocol QueryValueAndSubscribeConsumer
  (on-value [this x])
  (on-active-no-sow [this])

  ;; reason: invalid uri | cannot connect | connection was closed
  (on-inactive [this reason])

  (on-unaliased [this]))

(declare on-query-value-and-subscribe save-qvns)
(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [^String alias ^String filter ^String context-expr ^String value-expr consumer]
  (let [qvns-or-error (f/qvns-or-error filter context-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (do (save-qvns alias qvns-or-error)
          (on-query-value-and-subscribe alias)))))

(declare get-executor)
(defn- async
  [uri f & args]
  (.submit (get-executor uri) #(apply f args)))

(defn- function
  [kw]
  (resolve (symbol (name kw))))

(declare get-new-client uri->client)
(defn- get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (let [u->c (swap! uri->client #(if-not (% uri)
                                   (assoc % uri (get-new-client uri))
                                   %))]
    (u->c uri)))

(declare save-executor-if-absent state)
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

(declare notify)
(defn- new-msg-handler
  [sub]
  (reify MessageHandler
    (invoke [_ msg]
      (case (.getCommand msg)
        (Message$Command/SOW Message$Command/Publish)
        (let [m (.getData msg)
              qvns-in-scope-coll (clojure.core/filter
                                   #(f/in-scope? % m)
                                   (f-state/qvns-set @state sub))]
          (doseq [qvns qvns-in-scope-coll]
            (notify qvns m)))

        ;; pending
        Message$Command/OOF ,,,))))

(defn- notify
  [qvns m]
  )

(defn- on-aliased
  [a sub]
  )

(declare async revisit)
(defn- on-query-value-and-subscribe
  [alias]
  (let [sub (f-state/sub alias)]
    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) revisit alias)))

(declare state)
(defn- revisit
  "subscribes | replaces filter | unsubscribes - depending on the state

  Assumes no concurrency by subscription"
  [a]
  (let [[action-kw args] (f/revisit a @state)]
    (apply (function action-kw) args)))

(defn- save-ampsies
  [sub ampsies]
  (swap! state f-state/state-after-new-ampsies sub ampsies))

(defn- save-alias
  [a sub]
  (swap! state f-state/state-after-new-alias a sub))

(defn- save-executor-if-absent
  [uri executor]
  (swap! state f-state/state-after-new-executor-if-absent uri executor))

(defn- save-qvns
  [a qvns]
  (swap! state f-state/state-after-new-qvns a qvns))

(declare uniq-id)
(defn- subscribe
  [sub filter]
  (let [client     (get-client (:uri sub))
        sub-id     (uniq-id)
        command    (.. (Command. "sow_and_subscribe")
                       (setTopic (:topic sub))
                       (setSubId sub-id)
                       (setFilter filter))
        handler    (new-msg-handler sub)
        command-id (.executeAsync client command handler)]
    (save-ampsies sub (f/ampsies client command-id sub-id))))

(defn- uniq-id [] (str (java.util.UUID/randomUUID)))

(defn- unsubscribe
  [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom nil))

(def ^:private uri->client (atom nil))
