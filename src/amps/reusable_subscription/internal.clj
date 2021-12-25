(ns amps.reusable-subscription.internal
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.content-filter :as cf]))

;;
;; Ensures a single reusable subscription to a client/topic. 
;;
;; It keeps track of all the filters associated with a client/topic to
;; be used when replacing a filter on the single subscription. The
;; client needs only to add/remove filters.
;;

(defrecord Subscribe [])

(defrecord ReplaceFilter [filter sub-id])

(defrecord Unsubscribe [command-id])

(defn ensure [resubs client topic filter]
  (let [k [client topic]]
    (if-let [v (get resubs k)]
      (let [new-filter (cf/add (:filter v) filter)
            sub-id (:sub-id v)]
        [(assoc resubs k {:filter new-filter, :sub-id sub-id})
         (->ReplaceFilter new-filter sub-id)])
      [(assoc resubs k {:filter filter}) (->Subscribe)])))

(defn remove [resubs client topic filter]
  (let [k [client topic]
        v (get resubs k)]
    (if (= (:filter v) filter)
      [(dissoc resubs k) (->Unsubscribe (:command-id v))]
      (let [new-filter (cf/remove (:filter v) filter)]
        [(assoc-in resubs [k :filter] new-filter)
         (->ReplaceFilter new-filter (:sub-id v))]))))

(defn action [ensure-or-remove-return]
  (second ensure-or-remove-return))

(defn resubs [ensure-or-remove-return]
  (first ensure-or-remove-return))

(defn subscribed [resubs client topic sub-id command-id]
  (update resubs [client topic] merge {:sub-id sub-id, :command-id command-id}))

(defn failed-to-subscribe [resubs client topic]
  (dissoc resubs [client topic]))

(defn disconnected [resubs client]
  (loop [resubs resubs
         topics (map second (filter (comp #{client} first) (keys resubs)))]
    (when (seq topics)
      (recur (dissoc resubs [client (first topics)])
             (next topics)))))
