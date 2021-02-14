(ns simple-amps
  (:refer-clojure :exclude [alias filter])
  (:require [simple-amps.functional :as f]
            [simple-amps.functional.state :as f-state]
            [clojure.stacktrace])
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
  (.submit (get-executor uri)
           #(try (apply f args)
                 (catch Throwable ex
                   (clojure.stacktrace/print-cause-trace ex)))))

(defn- clone
  [s]
  (String. s))

(defn- function
  [kw]
  (resolve (symbol (name kw))))

(declare get-new-client save-client-if-absent state)
(defn- get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (let [c (f-state/client @state uri)]
    (if c
      c
      (let [new-c (get-new-client uri)
            _     (save-client-if-absent uri new-c)
            r     (f-state/client @state uri)]
        (when (not= r new-c)
          (.close new-c))
        r))))

(declare save-executor-if-absent state)
(defn- get-executor
  [uri]
  (let [e (f-state/executor @state uri)]
    (if e
      e
      (let [new-e (java.util.concurrent.Executors/newSingleThreadExecutor)]
        (save-executor-if-absent uri new-e)
        (f-state/executor @state uri)))))

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
          (str (java.util.UUID/randomUUID))))

(declare notify)
(defn- handle-json
  [json sub]
  (println "handle-json")
  (doseq [[value qvns] (f/handle-json json sub @state)]
    (notify qvns value)))

(declare async)
(defn- new-json-msg-handler
  [sub]
  (reify MessageHandler
    (invoke [_ msg]
      (let [cmd (.getCommand msg)]
        (cond 
          (#{Message$Command/SOW Message$Command/Publish} cmd)
          (do
            (println "new-json-msg-handler SOW or Publish")
            (let [json (clone (.getData msg))]
              (async (:uri sub) handle-json [json sub])))

          (= Message$Command/OOF cmd)
          (throw (UnsupportedOperationException.)))))))

(defn notify
  [qvns x]
  (on-value (:consumer qvns) x))

(defn- on-aliased
  [a sub]
  )

(declare async revisit)
(defn- on-query-value-and-subscribe
  [a]
  (let [sub (f-state/sub @state a)]
    
    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) revisit a)))

(declare state)
(defn- revisit
  "subscribes | replaces filter | unsubscribes - depending on the state

  Assumes no concurrency by subscription"
  [a]
  (let [[action-kw args] (f/revisit a @state)
        f (function action-kw)]
    (println "revisit - before apply")
    (println *ns* action-kw f args)
    (apply f args)
    (println "revisit - after apply")))

(defn- save-ampsies
  [sub ampsies]
  (swap! state f-state/state-after-new-ampsies sub ampsies)
  nil)

(defn- save-alias
  [a sub]
  (swap! state f-state/state-after-new-alias a sub))

(defn- save-client-if-absent
  [uri client]
  (swap! state f-state/state-after-new-client-if-absent uri client))

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
        handler    (new-json-msg-handler sub)
        command-id (.executeAsync client command handler)]
    (save-ampsies sub (f/ampsies client command-id sub-id))))

(defn- uniq-id [] (str (java.util.UUID/randomUUID)))

(defn- unsubscribe
  [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom nil))
