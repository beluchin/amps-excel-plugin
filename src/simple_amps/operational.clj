(ns simple-amps.operational
  (:require clojure.stacktrace
            logging
            [simple-amps.consumer :as c]
            [simple-amps.functional :as f]
            [simple-amps.functional.state :as f-state])
  (:import [com.crankuptheamps.client
            Client ClientDisconnectHandler Command Message$Command MessageHandler]
           [com.crankuptheamps.client.exception
            ConnectionException
            CommandException]))

(declare get-new-client-notify-qvns state state-save-client)
(defn get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (or (f-state/client @state uri) 
      (let [new-c (get-new-client-notify-qvns uri)]
        (state-save-client uri new-c)
        new-c)))

(declare async revisit)
(defn on-query-value-and-subscribe
  [a qvns]
  (if-let [sub (f-state/sub @state a)]

    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) revisit a)

    (c/on-inactive (:consumer qvns) "undefined alias")))

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

(declare get-executor)
(defn- async
  [^String uri f & args]
  (.submit
    (get-executor uri)
    #(try

       ;; otherwise *ns* is clojure.core !!
       (binding [*ns* (find-ns 'simple-amps.operational)]

         (apply f args))
       (catch Throwable ex
         (logging/error (with-out-str (clojure.stacktrace/print-cause-trace ex)))))))

(defn- executeAsync-n-get-command-id
  [client topic sub-id fi handler]
  (.executeAsync client
                 (.. (Command. "sow_and_subscribe")
                     (setTopic topic )
                     (setSubId sub-id)
                     (setFilter fi))
                 handler))

(defn- executeAsync-replacing-n-get-command-id
  [client topic sub-id fi handler]
  (.executeAsync client
                 (.. (Command. "sow_and_subscribe")
                     (setTopic topic )
                     (setSubId sub-id)
                     (setFilter fi)
                     (setOptions "replace"))
                 handler))

(defn- executeAsync-try-replacing-n-get-command-id
  [client topic sub-id command-id fi handler]
  (try (executeAsync-replacing-n-get-command-id client topic sub-id fi handler)
       (catch CommandException _
         (.unsubscribe client command-id)
         (executeAsync-n-get-command-id client topic sub-id fi handler))))

(declare notify-many state-delete)
(defn on-disconnected
  [client]
  (let [uri (str (.getURI client))
        consumer-coll (map :consumer (f/qvns-set @state uri))]
    (notify-many consumer-coll c/on-inactive "client disconnected")
    (logging/info (str "client disconnected: " uri))
    (state-delete client)))

(defn- clone
  [s]
  (String. s))

(defn- function
  [kw]
  (resolve (symbol (name kw))))

(declare state state-save-executor-if-absent)
(defn- get-executor
  [uri]
  (let [e (f-state/executor @state uri)]
    (if e
      e
      (let [new-e (java.util.concurrent.Executors/newSingleThreadExecutor)]
        (state-save-executor-if-absent uri new-e)
        (f-state/executor @state uri)))))

(declare client-disconnect-handler get-new-client-name)
(defn- get-new-client
  [uri]
  (doto (Client. (get-new-client-name))
    (.connect uri)
    (.setDisconnectHandler client-disconnect-handler)
    (.logon)))

(declare notify-many)
(defn- get-new-client-notify-qvns
  [uri]
  (let [consumer-coll (map :consumer (f/qvns-set @state uri))]
    (notify-many consumer-coll c/on-activating)
    (try (get-new-client uri)
    (catch ConnectionException ex
      (notify-many consumer-coll
                   c/on-inactive
                   (str "cannot connect: " (.getMessage ex)))
      nil))))

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

(defn- notify
  "a call to one consumer - protected against exceptions"
  [f c & args]
  (try (apply f c args)
       (catch Throwable ex
         (logging/error (with-out-str (clojure.stacktrace/print-cause-trace ex))))))

(defn- notify-many
  "eagerly notify each of the consumers on the collection"
  [coll f & args]
  (doseq [c coll] (apply notify f c args)))

(declare state-save uniq-id)
(defn- subscribe
  ([sub fi qvns-set]
   (when-let [c (get-client (:uri sub))] (subscribe sub fi qvns-set c)))
  ([sub fi qvns-set client]
   (let [sub-id     (uniq-id)
         command-id (executeAsync-n-get-command-id 
                      client
                      (:topic sub)
                      sub-id
                      fi
                      (new-json-msg-handler sub))]
     (state-save sub (f/ampsies client command-id sub-id) qvns-set)
     (notify-many (map :consumer qvns-set) c/on-activated))))

(declare state-save)
(defn- resubscribe
  [sub fi qvns-super-set qvns-set-to-activate ampsies]
  (notify-many (map :consumer qvns-set-to-activate) c/on-activating)
  (let [command-id (executeAsync-try-replacing-n-get-command-id
                     (:client ampsies)
                     (:topic sub)
                     (:sub-id ampsies)
                     (:command-id ampsies)
                     fi
                     (new-json-msg-handler sub))]
    (state-save sub (assoc ampsies :command-id command-id) qvns-super-set)
    (notify-many (map :consumer qvns-set-to-activate) c/on-activated)))

(declare state)
(defn- revisit
  [a]
  (when-let [[action args] (f/subscription-action+args a @state)]
    (apply (function action) args)))

(defn- state-delete
  [x]
  (swap! state f/state-after-delete x))

(defn- state-save
  [sub ampsies activated-qvns-set]
  (swap! state f-state/after sub ampsies activated-qvns-set))

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

(def ^:private client-disconnect-handler 
  (reify ClientDisconnectHandler
    (invoke [_ client]
      (async (str (.getURI client)) on-disconnected client))))
