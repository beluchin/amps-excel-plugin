(ns amps-excel-plugin.amps.functional)

(defn components
  [uri]
  (zipmap [:host-port :message-type]
          (rest (re-find #"tcp://([^/]+)/amps/([^/]+)" uri))))
