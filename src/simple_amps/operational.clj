(ns simple-amps.operational
  (:refer-clojure :exclude [filter])
  (:require clojure.stacktrace
            logging
            [simple-amps.consumer :as c]
            [simple-amps.functional :as f]
            [simple-amps.functional.state :as f-state])
  (:import [com.crankuptheamps.client Client ClientDisconnectHandler Command
            Message$Command MessageHandler]
           com.crankuptheamps.client.exception.ConnectionException))

(declare get-new-client save-client-if-absent state)
(defn get-client
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

(defn on-aliased
  [a sub]
  )

(declare async revisit)
(defn on-query-value-and-subscribe
  [a]
  (let [sub (f-state/sub @state a)]

    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) revisit a)))

(defn save-alias
  [a sub]
  (swap! state f-state/state-after-new-alias a sub))

(defn save-qvns
  [a qvns]
  (swap! state f-state/state-after-new-qvns a qvns))

(declare get-client-or-notify-qvns new-json-msg-handler save-ampsies uniq-id)
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
     (save-ampsies sub (f/ampsies client command-id sub-id)))))

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

(defn- get-client-or-notify-qvns
  [sub]
  (letfn [(notify-all-qvns [reason]
            (doseq [c (map :consumer (f-state/qvns-set @state sub))]
              (c/on-inactive c reason)))]
    (try (get-client (:uri sub))
         (catch ConnectionException ex
           (notify-all-qvns (str "cannot connect: " (.getMessage ex)))
           nil))))

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
  (doseq [[value qvns] (f/handle-json json sub @state)]
    (notify qvns value)))

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
  [qvns x]
  (c/on-value (:consumer qvns) x))

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

(defn- save-client-if-absent
  [uri client]
  (swap! state f-state/state-after-new-client-if-absent uri client))

(defn- save-executor-if-absent
  [uri executor]
  (swap! state f-state/state-after-new-executor-if-absent uri executor))

(defn- uniq-id [] (str (java.util.UUID/randomUUID)))

(defn- unsubscribe
  [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom nil))
