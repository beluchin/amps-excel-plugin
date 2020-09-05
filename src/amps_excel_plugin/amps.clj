(ns amps-excel-plugin.amps
  (:import [com.crankuptheamps.client Client Command MessageHandler]))

(declare get-new-client-name)

(defn get-new-client
  [uri]
  (doto (Client. (get-new-client-name))
    (.connect uri)
    (.logon)))

(defn get-new-client-name [] (.toString (java.util.UUID/randomUUID)))

(defn subscribe-json
  "assumes the uri is truly json i.e tcp://.../amps/json/..."
  [uri topic json-consumer]
  (let [client (get-new-client uri)
        command  (.. (Command. "subscribe") (setTopic topic))
        handler  (reify MessageHandler
                   (invoke [_ msg] (json-consumer (.getData msg))))]
    (.executeAsync client command handler)))
