(ns amps
  (:import [com.crankuptheamps.client
            Client
            ClientDisconnectHandler]))

(defn new-client [uri name client-disconnect-handler]
  (doto (Client. name)
    (.connect uri)
    (.setDisconnectHandler client-disconnect-handler)
    (.logon)))

(defn new-client-disconnect-handler [client-consumer]
  (reify ClientDisconnectHandler
      (invoke [_ client]
        (client-consumer client))))
