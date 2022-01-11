(ns amps.qvns.internal
  (:refer-clojure :exclude [ensure]))

(defrecord Disconnect [client])
(defrecord InitialSubscription [topic+content-filter--coll
                                activating-runnable-coll])
(defrecord ReplaceFilter [content-filter sub-id command-id])

(defn action [result])

(defn ensure [mgr qvns])

(defn mgr [result])

(defn remove [mgr qvns])

(defn subscribed [mgr uri topic content-filter sub-id command-id])
