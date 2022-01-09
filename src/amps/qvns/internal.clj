(ns amps.qvns.internal
  (:refer-clojure :exclude [ensure]))

(defrecord InitialSubscription [topic+content-filter--coll
                                activating-runnable-coll])

(defn action [mgr])

(defn ensure [mgr qvns])
