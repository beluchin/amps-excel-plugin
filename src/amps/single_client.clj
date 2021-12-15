(ns amps.single-client
  (:refer-clojure :exclude [ensure promise]))

(defn disconnected [cmgr uri]
  (dissoc cmgr uri))

(defn failed-to-connect [cmgr uri]
  (dissoc cmgr uri))

(defn ensure [cmgr uri promise]
  (merge {uri promise} cmgr))

(defn promise [cmgr uri]
  (cmgr uri))

(comment
  (import com.crankuptheamps.client.Client
          com.crankuptheamps.client.exception.AMPSException)
  (def uri "tcp://localhost:9007/amps/json")
  (def p (delay (doto (Client. "unique client name")
                  (.connect uri)
                  (.logon))))
  (def cmgr nil)
  (def cmgr' (ensure cmgr uri p))
  (def client @(promise cmgr' uri))
  (.close client))
