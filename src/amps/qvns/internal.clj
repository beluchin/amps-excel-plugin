(ns amps.qvns.internal
  (:refer-clojure :exclude [ensure remove]))

(defrecord Disconnect [client])
(defrecord InitialSubscription [topic+content-filter--coll
                                activating-runnable-coll])
(defrecord ReplaceFilter [content-filter sub-id command-id])

(defn action [result])

(defn ensure [mgr qvns]
  "may decide to make the initial subscription associated with other
  qvns's on the same uri i.e. different topic or even same topic /
  different content filter")

(defn mgr [result])

(defn remove [mgr qvns])

(defn subscribed [mgr uri topic content-filter sub-id command-id])
