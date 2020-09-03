(ns amps-excel-plugin.amps)

(defn new-client-name
  "a new unique client name"
  []
  (.toString (java.util.UUID/randomUUID)))

