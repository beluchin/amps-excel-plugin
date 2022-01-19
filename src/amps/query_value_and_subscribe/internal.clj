(ns amps.query-value-and-subscribe.internal
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.query-value-and-subscribe.qvns :as qvns]
            andor))

;; --- decisions
(defrecord ConsumeOof [x oof-consumer-coll])
(defrecord ConsumeValue [x value-consumer-coll])
(defrecord Disconnect [uri])
;; when acting on this decision, the amps client needs to be closed 
;; should all the subscriptions fail
(defrecord Subscribe [topic+content-filter--coll activating-runnable-coll])

(defrecord ReplaceFilter [content-filter sub-id command-id])
(defrecord Unsubscribe [command-id])
;; ---

(defn- new-state+Subscribe [state qvns]
  [(conj state qvns) (->Subscribe [(qvns/topic qvns) (qvns/content-filter qvns)]
                                  [(qvns/activating-runnable qvns)])])

(defn- subscribe? [state qvns]
  (contains? state qvns))

(defn consumed [state sub-id m]
  "returns state+ConsumeValue-coll")

(defn consumed-oof [state sub-id m]
  "returns state+ConsumeOof-coll")

(def decision second)

(defn ensure
  "may decide to make the initial subscription associated with other
  qvns's on the same uri i.e. different topic or even same topic /
  different content filter

  returns state+decision"
  [state qvns]
  (cond
    (subscribe? state qvns) (new-state+Subscribe state qvns)))

(defn disconnected [state uri]
  "to be called when the client is disconnected outside of the process
  either because of error or manually disconnected on the
  Galvanometer

  returns the state+inactive-reason-consumer-coll")

(defn failed-to-subscribe [state uri topic content-filter]
  "returns state+inactive-reason-consumer-coll")

(def state first)

(defn remove [state qvns])

(defn subscribed [state uri topic content-filter sub-id command-id]
  "returns (perhaps among other things) the collection of runnables to
  call to notify that the subscription is active

  returns state+activated-runnable-coll")
