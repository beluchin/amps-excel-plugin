(ns amps.qvns.internal-test
  (:refer-clojure :exclude [ensure])
  (:require [amps.qvns.internal :as sut]
            [clojure.test :as t]
            test-helpers))

(def ^:private action sut/action)

(defn- ensure
  ([mgr] (ensure mgr :qvns))
  ([mgr qvns] (sut/ensure mgr qvns)))

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

(defn- replace-filter []
  (sut/->ReplaceFilter :content-filter :sub-id :command-id))

(defn- subscribed [mgr]
  (sut/subscribed mgr :uri :topic :content-filter :sub-id :command-id))

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
                                  (ensure (qvns :item-filter :new-item-filter))
                                  action)))))

