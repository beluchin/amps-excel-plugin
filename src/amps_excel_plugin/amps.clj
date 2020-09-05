(ns amps-excel-plugin.amps
  (:import [com.crankuptheamps.client Client Command MessageHandler]))

(declare get-new-client get-new-client-name uri->client)

(defn get-client
  "returns a existing client if possible. Otherwise creates a new client"
  [uri]
  (let [u->c (swap! uri->client #(if-not (% uri)
                                   (assoc % uri (get-new-client uri))
                                   %))]
    (u->c uri)))

(defn get-new-client
  [uri]
  (doto (Client. (get-new-client-name))
    (.connect uri)
    (.logon)))

(defn get-new-client-name [] (.toString (java.util.UUID/randomUUID)))

(defn subscribe-json
  "assumes the uri is truly json i.e tcp://.../amps/json/..."
  [uri topic json-consumer]
  (let [client (get-client uri)
        command  (.. (Command. "subscribe") (setTopic topic))
        handler  (reify MessageHandler
                   (invoke [_ msg] (json-consumer (.getData msg))))]
    (.executeAsync client command handler)))

(def uri->client (atom {}))
