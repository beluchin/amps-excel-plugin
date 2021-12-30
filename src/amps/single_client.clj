(ns amps.single-client
  (:require amps
            [amps.single-client.internal :as internal]
            [clojure.tools.logging :as log]
            helpers))

(def ^:dynamic *disconnected-client-consumer* nil)
;;(alter-var-root #'*client-name* (constantly "amps-excel-plugin"))
(def ^:dynamic *client-name* nil)

(def ^:private cmgr (atom nil))
(def ^:private client-disconnect-handler
  (amps/new-client-disconnect-handler
    (fn [client]
      (let [uri (str (.getURI client))]
        (log/info uri (.getName client) "disconnected")
        (swap! cmgr internal/disconnected uri)
        (when-let [c *disconnected-client-consumer*]
          (c client))))))

(declare unique-name)
(defn- new-client-delay [uri]
  (delay (amps/new-client uri (unique-name) client-disconnect-handler)))

(declare unique-including user-name)
(defn- unique-name []
  (unique-including *client-name* "single-client" (user-name) (helpers/get-pid)))

(declare unique-string)
(defn- unique-including [& strs]
  (clojure.string/join "-" (filter (comp not nil?) (flatten [strs (unique-string)]))))

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

