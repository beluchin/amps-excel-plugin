(ns amps.qvns.internal-test
  (:refer-clojure :exclude [ensure])
  (:require [amps.qvns.internal :as sut]
            [clojure.test :as t]))

(def ^:private action sut/action)

(defn- ensure [mgr]
  (sut/ensure mgr :qvns))

(defn- initial-subscription []
  (sut/->InitialSubscription [[:topic :content-filter]] [:activating-runnable]))

(t/deftest ensure-test
  (t/testing "initial subscription"
    (t/is (= (initial-subscription) (-> nil
                                        ensure
                                        action)))))

