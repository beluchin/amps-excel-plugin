(ns amps.qvns.internal-test
  (:refer-clojure :exclude [ensure])
  (:require [amps.qvns.internal :as sut]
            [clojure.test :as t]))

(def ^:private action sut/action)

(defn- ensure
  ([mgr] (ensure mgr :qvns))
  ([mgr qvns] (sut/ensure mgr qvns)))

(defn- initial-subscription []
  (sut/->InitialSubscription [[:topic :content-filter]] [:activating-runnable]))

(def ^:private mgr sut/mgr)

(defn- qvns [& k+v--coll]
  (throw (UnsupportedOperationException.)))

(defn- replace-filter []
  (throw (UnsupportedOperationException.)))

(defn- subscribed [mgr]
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
                                  (ensure (qvns :item-filter :new-item-filter))
                                  action)))))

