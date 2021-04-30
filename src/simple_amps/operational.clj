(ns simple-amps.operational
  (:refer-clojure :exclude [alias])
  (:require clojure.stacktrace
            logging
            functional
            [simple-amps.consumer :as c]
            [simple-amps.functional :as f]
            [simple-amps.functional.state :as s]
            [amps-excel-plugin.functional :as functional])
  (:import [com.crankuptheamps.client
            Client ClientDisconnectHandler Command Message$Command MessageHandler]
           [com.crankuptheamps.client.exception
            ConnectionException
            CommandException]))

(declare get-conn-state-agent revisit-conn)
(defn async-revisit-conn [alias]
  (send-off (get-conn-state-agent alias) revisit-conn alias))

(declare get-new-client-notify-qvns state state-save-client)
(defn get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (or (s/client @state uri) 
      (when-let [new-c (get-new-client-notify-qvns uri)]
        (state-save-client uri new-c)
        new-c)))

(declare async execute-qvns-action)
(defn on-query-value-and-subscribe [alias qvns]
  (if-let [sub (s/sub @state alias)]

    ;; no blocking calls on the thread where the excel functions are called.
    (async (:uri sub) execute-qvns-action alias)

    (c/on-inactive (:consumer qvns) "undefined alias")))

(declare execute-unsuscribe-action)
(defn on-unsubscribed [[alias qvns]]
  (when-let [sub (s/sub @state alias)]
    (let [uri (:uri sub)]
      ;; no blocking calls on the thread where the excel functions are called.
      (async uri execute-unsuscribe-action alias uri))))

(defn save
  [a sub]
  (swap! state s/state-after-new-alias a sub))

(defn put-qvns
  [a qvns id]
  (swap! state #(-> %
                    (s/after-new-alias-qvns a qvns)
                    (s/after-new-id-alias+qvns id [a qvns]))))

(defn remove-qvns-call-id [x]
  (when-let [alias+qvns (s/alias+qvns @state x)]
    (swap! state f/state-after-remove-qvns-call-id x)
    alias+qvns))

(defn- get-conn-state-agent [alias]
  (or (s/conn-state-agent @state alias) 
      (do
        (swap! state s/after-new-conn-agent-if-absent alias (agent nil))
        (s/conn-state-agent @state alias))))

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
                     (setFilter fi)
                     (setOptions "oof"))
                 handler))

(defn- executeAsync-replacing-n-get-command-id
  [client topic sub-id fi handler]
  (.executeAsync client
                 (.. (Command. "sow_and_subscribe")
                     (setTopic topic )
                     (setSubId sub-id)
                     (setFilter fi)
                     (setOptions "oof,replace"))
                 handler))

(defn- executeAsync-try-replacing-n-get-command-id
  [client topic sub-id command-id fi handler]
  (try (executeAsync-replacing-n-get-command-id client topic sub-id fi handler)
       (catch CommandException _
         (.unsubscribe client command-id)
         (executeAsync-n-get-command-id client topic sub-id fi handler))))

(declare notify-many remove-client)
(defn on-disconnected
  [client]
  (let [uri (str (.getURI client))
        consumer-coll (map :consumer (f/qvns-set @state uri))]
    (notify-many consumer-coll c/on-inactive "client disconnected")
    (logging/info (str "client disconnected: " uri))
    (remove-client client)))

(defn- clone
  [s]
  (String. s))

(declare function state)
(defn- execute-qvns-action [alias]
  (when-let [[action args] (f/new-qvns-action+args alias @state)]
    (apply (function action) args)))

(declare state)
(defn- execute-unsuscribe-action [alias uri]
  (when-let [[client new-state] (f/client-to-close+state @state alias uri)]
    (.close client)
    (reset! state new-state)))

(defn- function
  [kw]
  (resolve (symbol (name kw))))

(declare state state-save-executor-if-absent)
(defn- get-executor [uri]
  (let [e (s/executor @state uri)]
    (if e
      e
      (let [new-e (java.util.concurrent.Executors/newSingleThreadExecutor)]
        (state-save-executor-if-absent uri new-e)
        (s/executor @state uri)))))

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

(declare notify)
(defn- handle-json-oof
  [json sub]
  (doseq [[value qvns] (f/handle-json json sub @state)]
    (notify c/on-oof (:consumer qvns) value)))

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
            (let [json (clone (.getData msg))]
              (async uri handle-json-oof json sub))))))))

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

(defn- remove-client [c]
  (swap! state f/state-after-remove-client c))

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

(defn- state-save
  [sub ampsies activated-qvns-set]
  (swap! state #(-> %
                    (s/after-new-sub-ampsies sub ampsies)
                    (s/after-new-sub-activated-qvns-set sub activated-qvns-set))))

(defn- state-save-client
  [uri client]
  (swap! state s/state-after-new-client uri client))

(defn- state-save-executor-if-absent
  [uri executor]
  (swap! state s/after-new-executor-if-absent uri executor))

(defn- uniq-id [] (str (java.util.UUID/randomUUID)))

(defn- unsubscribe [subscription]
  (let [{:keys [::client ::command-id]} subscription]
    (.unsubscribe client command-id)))

(def ^:private state (atom nil))

(def ^:private client-disconnect-handler 
  (reify ClientDisconnectHandler
    (invoke [_ client]
      (async (str (.getURI client)) on-disconnected client))))
