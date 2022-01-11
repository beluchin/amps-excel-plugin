(ns amps.qvns.internal-test
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.qvns.internal :as sut]
            [clojure.test :as t]
            test-helpers))

(def ^:private action sut/action)

(defn- disconnect []
  (sut/->Disconnect :client))

(defn- ensure
  ([mgr] (ensure mgr :qvns))
  ([mgr qvns] (sut/ensure mgr qvns)))

(declare mgr subscribed)
(defn- ensured-subscription-mgr []
  (-> nil ensure mgr subscribed mgr))

(defn- initial-subscription []
  (sut/->InitialSubscription [[:topic :content-filter]] [:activating-runnable]))

(def ^:private mgr sut/mgr)

(defn- qvns [& {:as overrides}]
  (merge (test-helpers/map-of-keywords uri
                                       topic
                                       context-filter
                                       item-filter
                                       value-extractor
                                       event-handlers)
         overrides))

(defn- remove
  ([mgr] (remove mgr :qvns))
  ([mgr qvns] (sut/remove mgr qvns)))

(defn- replace-filter []
  (sut/->ReplaceFilter :content-filter :sub-id :command-id))

(defn- subscribed [mgr]
  (sut/subscribed mgr :client :topic :content-filter :sub-id :command-id))

(defn- unsubscribe [mgr]
  (throw (UnsupportedOperationException.)))

(t/deftest ensure-test
  (t/testing "initial subscription"
    (t/is (= (initial-subscription) (-> nil
                                        ensure
                                        action))))

  (t/testing "replace filter"
    (t/is (= (replace-filter) (-> nil
                                  ensure
                                  mgr
                                  subscribed
                                  mgr
                                  (ensure (qvns :item-filter :new-item-filter))
                                  action)))))


(t/deftest remove-test 
  (t/testing "disconnect"
    (t/is (= (disconnect)
             (-> (ensured-subscription-mgr)
                 remove
                 action))))

  (t/testing "unsubscribe - different topic" 
    (t/is (= (unsubscribe)
             (-> (ensured-subscription-mgr)
                 (ensure (qvns :client :client :topic :topic-y))
                 mgr
                 subscribed
                 mgr
                 (remove (qvns :client :client :topic :topic-y))
                 action)))))

