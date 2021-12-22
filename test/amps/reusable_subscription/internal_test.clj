(ns amps.reusable-subscription.internal-test
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.reusable-subscription.internal :as sut]
            [clojure.test :as t]))

(defn- ensure [resubs new-filter]
  (sut/ensure resubs :client :topic new-filter))

(defn- failed-to-subscribe [resubs]
  (sut/failed-to-subscribe resubs :client :topic))

(defn- fresh-ensure []
  (sut/ensure nil :client :topic :filter))

(defn- pending-filter-replacement
  ([filter]
   (pending-filter-replacement filter :command-id))
  ([filter command-id]
   (sut/->PendingFilterReplacement filter :sub-id command-id)))

(defn- pending-subscription []
  (sut/->PendingSubscription :filter))

(defn- pending-unsubscribe []
  (sut/->PendingUnsubscribe :command-id))

(defn- remove
  ([resubs]
   (remove resubs :filter))
  ([resubs filter]
   (sut/remove resubs :client :topic filter)))

(defn- subscribed
  ([resubs]
   (subscribed resubs :filter :command-id))
  ([resubs filter]
   (subscribed resubs filter :command-id))
  ([resubs filter command-id]
   (sut/subscribed resubs :client :topic filter :sub-id command-id)))

(defn- whats-pending [resubs]
  (sut/whats-pending resubs :client :topic))

(t/deftest ensure-test
  (t/testing "a new subscription"
    (t/is (= (pending-subscription)
             (whats-pending (fresh-ensure)))))

  (t/testing "updating the filter"
    (t/testing "adding a new filter"
      (t/is (= (pending-filter-replacement (andor/or :new-filter :filter))
               (-> (fresh-ensure)
                   (subscribed)
                   (ensure :new-filter)
                   (whats-pending)))))))

(t/deftest failed-to-subscribe-test 
  (t/is (nil? (-> (fresh-ensure)
                  (failed-to-subscribe)
                  (whats-pending)))) )

(t/deftest remove-test
  (t/testing "the only filter"
    (t/is (= (pending-unsubscribe)
             (-> (fresh-ensure)
                 (subscribed)
                 (remove)
                 (whats-pending)))))

  (t/testing "one of many"
    (t/is (= (pending-filter-replacement :filter :command-id-2)
             (-> (fresh-ensure)
                 (subscribed :filter :command-id-1)
                 (ensure :new-filter)
                 (subscribed (andor/or :filter :new-filter) :command-id-2)
                 (remove :new-filter)
                 (whats-pending))))))
