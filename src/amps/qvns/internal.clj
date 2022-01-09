(ns amps.qvns.internal
  (:refer-clojure :exclude [ensure]))

(defrecord InitialSubscription [topic+content-filter--coll
                                activating-runnable-coll])

(defn action [result])

(defn ensure [mgr qvns])

(defn mgr [result])
