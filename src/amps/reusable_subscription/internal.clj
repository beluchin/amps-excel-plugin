(ns amps.reusable-subscription.internal
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.content-filter :as cf]))

;;
;; Ensures a single reusable subscription to a client/topic. 
;;
;; It keeps track of all the filters associated with a client/topic to
;; be used when replacing a filter on the single subscription to the
;; client/topic. The client needs only to add/remove filters.
;;

(defrecord ^:private Subscribed [filter sub-id command-id])

(defn- check-for-ensure [state client topic]
  state)

(defn- check-for-failed-to-subscribe [state client topic]
  state)

(defn- check-for-removal [state client topic filter]
  state)

(defn- check-for-subscribed [state client topic filter sub-id]
  state)

(declare ->PendingFilterReplacement)
(defn- pending-filter-replacement [state new-filter]
  (->PendingFilterReplacement new-filter
                              (:sub-id state)
                              (:command-id state)))

(declare ->PendingSubscription)
(defn- pending-subscription [filter]
  (->PendingSubscription filter))

(declare ->PendingUnsubscribe)
(defn- pending-unsubscribe [state]
  (->PendingUnsubscribe (:command-id state)))

(defrecord PendingSubscription [filter])

(defrecord PendingFilterReplacement [filter sub-id command-id])

(defrecord PendingUnsubscribe [command-id])

(defn ensure
  "the library keeps track of the filter (andor/optimize'd) to use in
  replacements.  Client needs only to supply the new filter they want
  to subscribe to. Returns a new state of the reusable subscriptions"
  [resubs client topic filter]
  (let [k [client topic]]
    (if-let [state (get resubs k)]
      (-> (check-for-ensure state client topic)
          (pending-filter-replacement (cf/add (:filter state) filter))
          (#(assoc resubs k %)))
      (assoc resubs k (pending-subscription filter)))))

(defn remove
  "returns a new state of the reusable subscriptions"
  [resubs client topic filter]
  (let [k [client topic]
        state (get resubs k)]
    (check-for-removal state client topic filter)
    (if (= (:filter state) filter)
      (assoc resubs k (pending-unsubscribe state))
      (assoc resubs k (pending-filter-replacement
                        state
                        (cf/remove (:filter state) filter))))))

(defn whats-pending
  "returns one of the pending states or nil if nothing is pending"
  [resubs client topic]
  (get resubs [client topic]))

(defn subscribed
  "returns a new state of the reusable subscriptions. Throws if a
  subscription or a filter replacement was not pending for the given filter"
  [resubs client topic filter sub-id command-id]
  (check-for-subscribed (get resubs [client topic]) client topic filter sub-id)
  (assoc resubs [client topic] (->Subscribed filter sub-id command-id)))

(defn failed-to-subscribe
  "returns a new state of the reusable subscriptions. Throws if a
  subscription was not pending for the given filter. Calling it while
  pending a filter replacement is illegal since the sensible thing to do
  when failing to replace the filter is to unsubscribe"
  [resubs client topic]
  (check-for-failed-to-subscribe (get resubs [client topic]) client topic)
  (dissoc resubs [client topic]))

(defn disconnected
  "returns a new state of the reusable subscriptions"
  [resubs client])
