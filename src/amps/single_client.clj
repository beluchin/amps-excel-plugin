(ns amps.single-client
  (:require amps
            [amps.single-client.internal :as internal]
            helpers))

(def ^:private cmgr (atom nil))
(def ^:private disconnected-client-consumer (atom nil))
(def ^:private client-disconnect-handler
  (amps/new-client-disconnect-handler
    (fn [client]
      (swap! cmgr internal/disconnected (str (.getURI client)))
      (when-let [c @disconnected-client-consumer]
        (c client)))))

(declare unique-name)
(defn- new-client-delay [uri]
  (delay (amps/new-client uri (unique-name) client-disconnect-handler)))

(declare unique-including user-name)
(defn- unique-name []
  (unique-including "single-client" (user-name) (helpers/get-pid)))

(declare unique-string)
(defn- unique-including [& strs]
  (clojure.string/join "-" (flatten [strs (unique-string)])))

(defn- unique-string []
  (java.util.UUID/randomUUID))

(defn- user-name []
  (System/getProperty "user.name"))

(defn closed [client]
  (swap! cmgr internal/disconnected (str (.getURI client)))
  nil)

(defn get-client [uri]
  (if-let [p (internal/promise @cmgr uri)]
    @p
    (-> cmgr
        (swap! internal/ensure uri (new-client-delay uri))
        (internal/promise uri)
        deref)))

(defn set-disconnected-client-consumer [c]
  (reset! disconnected-client-consumer c)
  nil)
