(ns amps.qvns.internal
  (:refer-clojure :exclude [ensure remove]))

(defrecord ConsumeValue [x])
(defrecord Disconnect [uri])
(defrecord InitialSubscription [topic+content-filter--coll
                                activating-runnable-coll])
(defrecord ReplaceFilter [content-filter sub-id command-id])
(defrecord Unsubscribe [command-id])

(defn action [result])

(defn ensure [state qvns]
  "may decide to make the initial subscription associated with other
  qvns's on the same uri i.e. different topic or even same topic /
  different content filter")

(defn disconnected [state uri])

(defn failed-to-subscribe [state uri topic content-filter])

(defn state [result])

(defn remove [state qvns])

(defn subscribed [state uri topic content-filter sub-id command-id]
  "returns (perhaps among other things) the collection of runnables to
  call to notify that the subscription is active")
