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
(defrecord Subscribe [topic+content-filter->callback-set])

(defrecord ReplaceFilter [content-filter sub-id command-id])
(defrecord Unsubscribe [command-id])
;; ---

(defn- add [state qvns]
  (update state :qvns-set (fnil conj #{}) qvns))

(defn- new? [state qvns]
  (not (contains? (:all-qvns state) qvns)))

(defn- new-state+Subscribe [state uri]
  (let [msg-stream (qvns/msg-stream qvns)]
    [state
     (->Subscribe
       (conj (topic+content-filter->callback-set--diff-msg-stream
               state
               msg-stream)
             (topic+content-filter+callback-set--same-msg-stream
               state
               msg-stream)))]))

(defn- subscribe? [state qvns]
  (or (new? state qvns)))

(declare qvns-coll-to-subscribe)
(defn- msg-stream->qvns-coll--to-subscribe
  [state uri]
  (let [qvns-coll (qvns-coll-to-subscribe state uri)]
    (group-by qvns/msg-stream qvns-coll)))

(defn- pending-subscription-result-qvns-coll [state uri])

(defn- qvns-coll [state uri]
  (filter #(= uri (qvns/uri %)) (:all-qvns state)))

(declare subscribed-qvns-coll)
(defn- qvns-coll-to-subscribe [state uri]
  (clojure.set/difference (qvns-coll state uri)
                          (subscribed-qvns-coll state uri)
                          (pending-subscription-result-qvns-coll state uri)))

(defn consumed [state sub-id m]
  "returns state+ConsumeValue-coll")

(defn consumed-oof [state sub-id m]
  "returns state+ConsumeOof-coll")

(def decision second)

(defn ensure
  "may decide to make the initial subscription associated with other
  qvns's on the same uri i.e. different topic or even same topic but
  different content filter

  returns state+decision"
  [state qvns]
  (let [state' (add state qvns)]
    (cond
      (subscribe? state' qvns) (new-state+Subscribe state' (qvns/uri qvns)))))

(defn disconnected [state uri]
  "to be called when the client is disconnected outside of the process
  either because of error or manually disconnected on the
  Galvanometer

  returns the state+inactive-reason-consumer-coll")

(defn failed-to-subscribe [state uri topic content-filter] state)

(def state first)

(defn remove [state qvns])

(defn subscribed [state uri topic content-filter sub-id command-id]
  state)
