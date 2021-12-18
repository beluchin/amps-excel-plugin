(ns amps.reusable-subscription.internal
  (:refer-clojure :exclude [ensure remove]))

(defrecord PendingSubscription [filter])

(defrecord PendingResubscription [subid filter previous-command-id])

(defrecord PendingUnsubscribe [command-id])

(defn ensure
  "returns a new state of the reusable subscriptions"
  [resubs client topic filter])

(defn remove
  "returns a new state of the reusable subscriptions"
  [resubs client topic filter])

(defn whats-pending
  "returns one of the pending states or nil if nothing is pending"
  [resubs client topic])

(defn subscribed
  "returns a new state of the reusable subscriptions. Throws if a
  re/subscription was not pending for the given filter"
  [resubs client topic filter sub-id command-id])

(defn unsubscribed
  "returns a new state of the reusable subscriptions. Throws if a
  resubscription/unsubscribe was not pending for the given filter"
  [resubs client topic filter command-id])

(defn failed-to-subscribe
  "returns a new state of the reusable subscriptions. Throws if a
  subscription was not pending for the given filter. Calling it while
  pending a resubscription is illegal since the sensible thing to do
  when failing to resubscribe is to unsubscribe"
  [resubs client topic])

(defn disconnected
  "returns a new state of the reusable subscriptions"
  [resubs client])
