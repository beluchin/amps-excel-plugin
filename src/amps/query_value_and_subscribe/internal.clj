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

(defn add [state qvns]
  (update state :all-qvns (fnil conj #{}) qvns))

(defn new? [state qvns]
  (not (contains? (:all-qvns state) qvns)))

(declare topic+content-filter+callback-set--same-msg-stream
         topic+content-filter->callback-set--diff-msg-stream)
(defn- new-state+Subscribe [state qvns]
  (let [state' (add state qvns)
        msg-stream (qvns/msg-stream qvns)]
    [state'
     (->Subscribe
       (conj (topic+content-filter->callback-set--diff-msg-stream
               state'
               msg-stream)
             (topic+content-filter+callback-set--same-msg-stream
               state'
               msg-stream)))]))

(defn- subscribe? [state qvns]
  (or (new? state qvns)))

(defn- topic+content-filter+callback-set--same-msg-stream [qvns-coll msg-stream]
  (let [qvns-coll' (filter #(#{msg-stream} (qvns/msg-stream %)) qvns-coll)]
    [
     ;; topic+content-filter
     [(:topic msg-stream)
      (andor/and (:filter-expr msg-stream)
                 (apply andor/or (map qvns/qvns-filter-expr qvns-coll')))]

     ;; callbacks
     (into #{} (map qvns/callbacks qvns-coll'))]))

;; O(n^2) - consider an improvement
(defn- topic+content-filter->callback-set--diff-msg-stream [qvns-seq msg-stream]
  (let [msg-streams (->> qvns-seq
                         (map qvns/msg-stream)
                         (filter (partial not= msg-stream))
                         ;; only those msg stream for the same uri
                         (filter #{(:uri msg-stream)}))]
    {}))

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

(defn failed-to-subscribe [state uri topic content-filter] state)

(def state first)

(defn remove [state qvns])

(defn subscribed [state uri topic content-filter sub-id command-id]
  state)
