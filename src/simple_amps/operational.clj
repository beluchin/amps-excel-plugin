(ns simple-amps.operational
  (:refer-clojure :exclude [filter])
  (:require clojure.stacktrace
            logging
            [simple-amps.consumer :as c]
            [simple-amps.functional :as f]
            [simple-amps.functional.state :as f-state])
  (:import [com.crankuptheamps.client Client ClientDisconnectHandler Command Message$Command MessageHandler]
           com.crankuptheamps.client.exception.ConnectionException))

(declare get-new-client state state-save-client)
(defn get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (or (f-state/client @state uri) 
      (let [new-c (get-new-client uri)]
        (state-save-client uri new-c)
        new-c)))

(declare async revisit)
(defn on-query-value-and-subscribe
  [a]
  (when-let [sub (f-state/sub @state a)]

    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) revisit a)))

(defn on-require
  [a sub]
  ;; no blocking calls on the thread where the excel functions are called.
  (async (:uri sub) revisit a))

(defn save-alias
  [a sub]
  (swap! state f-state/state-after-new-alias a sub))

(defn save-qvns
  [a qvns]
  (swap! state f-state/state-after-new-qvns a qvns))

(declare get-client-or-notify-qvns new-json-msg-handler state-save-ampsies uniq-id)
(defn subscribe
  ([sub filter]
   (when-let [c (get-client-or-notify-qvns sub)]
     (subscribe sub filter c)))
  ([sub filter client]
   (let [sub-id     (uniq-id)
         command    (.. (Command. "sow_and_subscribe")
                        (setTopic (:topic sub))
                        (setSubId sub-id)
                        (setFilter filter))
         handler    (new-json-msg-handler sub)
         command-id (.executeAsync client command handler)]
     (state-save-ampsies sub (f/ampsies client command-id sub-id)))))

(declare notify state-delete)
(def client-disconnect-handler 
  (reify ClientDisconnectHandler
    (invoke [_ client]
      (logging/info (str "client disconnected: " (.getURI client)))
      (doseq [qvns (f-state/qvns-set @state client)]
        (notify c/on-inactive (:consumer qvns) "client disconnected"))
      (state-delete client))))

(declare get-executor)
(defn- async
  [uri f & args]
  (.submit
    (get-executor uri)
    #(try

       ;; otherwise *ns* is clojure.core !!
       (binding [*ns* (find-ns 'simple-amps.operational)]

         (apply f args))
       (catch Throwable ex
         (logging/error (with-out-str (clojure.stacktrace/print-cause-trace ex)))))))

(defn- clone
  [s]
  (String. s))

(defn- function
  [kw]
  (resolve (symbol (name kw))))

(declare notify)
(defn- get-client-or-notify-qvns
  [sub]
  (letfn [(notify-all-qvns [reason]
            (doseq [c (map :consumer (f-state/qvns-set @state sub))]
              (notify c/on-inactive c reason)))]
    (try (get-client (:uri sub))
         (catch ConnectionException ex
           (notify-all-qvns (str "cannot connect: " (.getMessage ex)))
           nil))))

(declare state state-save-executor-if-absent)
(defn- get-executor
  [uri]
  (let [e (f-state/executor @state uri)]
    (if e
      e
      (let [new-e (java.util.concurrent.Executors/newSingleThreadExecutor)]
        (state-save-executor-if-absent uri new-e)
        (f-state/executor @state uri)))))

(declare get-new-client-name)
(defn- get-new-client
  [uri]
  (doto (Client. (get-new-client-name))
    (.connect uri)
    (.setDisconnectHandler client-disconnect-handler)
    (.logon)))

(defn- get-new-client-name
  []
  (format "%s:amps-excel-plugin:%s"
          (System/getProperty "user.name")
          (str (java.util.UUID/randomUUID))))

(declare notify)
(defn- handle-json
  [json sub]
  (doseq [[value qvns] (f/handle-json json sub @state)]
    (notify c/on-value (:consumer qvns) value)))

(declare async)
(defn- new-json-msg-handler
  [sub]
  (let [uri (:uri sub)
        sow-or-pub #{Message$Command/SOW Message$Command/Publish}]
    (reify MessageHandler
      (invoke [_ msg]
        (let [cmd (.getCommand msg)]
          (cond
            (sow-or-pub cmd)
            (let [json (clone (.getData msg))]
              (async uri handle-json json sub))

            (= Message$Command/OOF cmd)
            (throw (UnsupportedOperationException.))))))))

(defn notify
  [f & args]
  (try (apply f args)
       (catch Throwable ex
         (logging/error (with-out-str (clojure.stacktrace/print-cause-trace ex))))))

(declare state)
(defn- revisit
  "subscribes | replaces filter | unsubscribes - depending on the state

  Assumes no concurrency by subscription"
  [a]
  (when-let [[action-kw args] (f/revisit a @state)]
    (apply (function action-kw) args)))

(defn- state-delete
  [x]
  (swap! state f/state-after-delete x))

(defn- state-save-ampsies
  [sub ampsies]
  (swap! state f-state/state-after-new-ampsies sub ampsies))

(defn- state-save-client
  [uri client]
  (swap! state f-state/state-after-new-client uri client))

(defn- state-save-executor-if-absent
  [uri executor]
  (swap! state f-state/state-after-new-executor-if-absent uri executor))

(defn- uniq-id [] (str (java.util.UUID/randomUUID)))

(defn- unsubscribe
  [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom nil))

