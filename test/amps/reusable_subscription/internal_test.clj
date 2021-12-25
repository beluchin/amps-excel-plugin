(ns amps.reusable-subscription.internal-test
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.reusable-subscription.internal :as sut]
            [clojure.test :as t]))

(defn- disconnected [resubs]
  (sut/disconnected resubs :client))

(defn- ensure
  ([resubs]
   (ensure resubs :filter))
  ([resubs filter]
   (sut/ensure resubs :client :topic filter)))

(declare fresh-ensure subscribed)
(defn- ensure-filter-replacement []
  (-> (fresh-ensure)
      sut/resubs
      subscribed
      (ensure :new-filter)))

(defn- failed-to-subscribe [resubs]
  (sut/failed-to-subscribe resubs :client :topic))

(defn- fresh-ensure []
  (sut/ensure nil :client :topic :filter))

(defn- replace-filter [filter]
  (sut/->ReplaceFilter filter :sub-id))

(defn- remove
  ([resubs]
   (remove resubs :filter))
  ([resubs filter]
   (sut/remove resubs :client :topic filter)))

(declare subscribed)
(defn- resubs-after-fresh-subscribe []
  (-> (fresh-ensure)
      sut/resubs
      subscribed))

(defn- subscribe []
  (sut/->Subscribe))

(defn- subscribed
  ([resubs]
   (subscribed resubs :command-id))
  ([resubs command-id]
   (sut/subscribed resubs :client :topic :sub-id command-id)))

(defn- unsubscribe []
  (sut/->Unsubscribe :command-id))

(t/deftest ensure-test
  (t/testing "a new subscription"
    (t/is (= (subscribe) (sut/action (fresh-ensure)))))

  (t/testing "updating the filter"
    (t/is (= (replace-filter (andor/or :new-filter :filter))
             (-> (resubs-after-fresh-subscribe)
                 (ensure :new-filter)
                 sut/action)))))

(t/deftest failed-to-subscribe-test
  (t/testing "after the first subscription"
    (t/is (= (subscribe) (-> (fresh-ensure)
                             sut/resubs
                             failed-to-subscribe
                             ensure
                             sut/action))))

  (t/testing "after having replaced the filter"
    (t/is (= (subscribe) (-> (ensure-filter-replacement)
                             sut/resubs
                             failed-to-subscribe
                             ensure
                             sut/action)))))

(t/deftest remove-test
  (t/testing "the only filter"
    (t/is (= (unsubscribe)
             (-> (resubs-after-fresh-subscribe)
                 remove
                 sut/action))))

  (t/testing "one of many"
    (t/is (= (replace-filter :new-filter)
             (-> (resubs-after-fresh-subscribe)
                 (ensure :new-filter)
                 sut/resubs
                 subscribed
                 (remove :filter)
                 sut/action)))))

(t/deftest disconnected-test
  (t/is (= (subscribe)
           (-> (resubs-after-fresh-subscribe)
               disconnected
               ensure
               sut/action))))
