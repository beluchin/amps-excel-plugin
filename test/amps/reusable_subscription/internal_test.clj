(ns amps.reusable-subscription.internal-test
  (:require [amps.reusable-subscription.internal :as sut]
            [clojure.test :as t]))

(t/deftest ensure-test
  (t/testing "a new subscription"
    (t/is (= {[:client :topic] (sut/->PendingSubscription :filter)}
             (sut/ensure nil :client :topic :filter)))))
