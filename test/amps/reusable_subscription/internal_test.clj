(ns amps.reusable-subscription.internal-test
  (:require [amps.reusable-subscription.internal :as sut]
            [clojure.test :as t]))

(t/deftest ensure-test
  (t/testing "a new subscription"
    (t/is (= (sut/->PendingSubscription :filter)
             (sut/whats-pending (sut/ensure nil :client :topic :filter) 
                                :client
                                :topic))))

  (t/testing "updating the filter"
    (t/testing "adding a new filter"
      (t/is (= (sut/->PendingFilterReplacement (andor/or :new-filter :filter)
                                               :sub-id
                                               :command-id)
               (-> (sut/ensure nil :client :topic :filter)
                   (sut/subscribed :client :topic :filter :sub-id :command-id)
                   (sut/ensure :client :topic :new-filter)
                   (sut/whats-pending :client :topic)))))))

(t/deftest failed-to-subscribe-test 
  (t/is (nil? (-> (sut/ensure nil :client :topic :filter)
                  (sut/failed-to-subscribe :client :topic)
                  (sut/whats-pending :client :topic)))) )

(t/deftest remove-test
  (t/testing "the only filter"
    (t/is (= (sut/->PendingUnsubscribe :command-id)
             (-> (sut/ensure nil :client :topic :filter)
                 (sut/subscribed :client :topic :filter :subid :command-id)
                 (sut/remove :client :topic :filter)
                 (sut/whats-pending :client :topic)))))

  (t/testing "one of many"
    (t/is (= (sut/->PendingFilterReplacement :filter :subid :command-id-2)
             (-> (sut/ensure nil :client :topic :filter)
                 (sut/subscribed :client :topic :filter :subid :command-id-1)
                 (sut/ensure :client :topic :new-filter)
                 (sut/subscribed :client
                                 :topic
                                 (andor/or :filter :new-filter)
                                 :subid
                                 :command-id-2)
                 (sut/remove :client :topic :new-filter)
                 (sut/whats-pending :client :topic))))))

(t/deftest unsubscribed-test
  (t/is (nil? (-> (sut/ensure nil :client :topic :filter)
                  (sut/subscribed :client :topic :filter :sub-id :command-id)
                  (sut/ensure :client :topic :new-filter)
                  (sut/unsubscribed :client :topic :filter :command-id)
                  (sut/whats-pending :client :topic)))))
