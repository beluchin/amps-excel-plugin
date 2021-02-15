(ns simple-amps.operational-test
  (:require [simple-amps.operational :as sut]
            [clojure.test :as t]
            [simple-amps.functional.state :as f-state]
            [simple-amps.consumer :as c]))

(declare f)
(t/deftest subscribe-test
  (t/testing "qvns' are notified if it cannot connect"
    (let [consumer-called? (atom false)]
        (with-redefs [f-state/qvns-set
                      #(when (= [:sub] %&)
                         [{:consumer
                           (reify c/QueryValueAndSubscribeConsumer
                             (on_inactive [_ _] (reset! consumer-called? true)))}])

                      sut/get-client
                      (fn [& _] (throw (RuntimeException.)))]
          (sut/subscribe :sub :filter)
          (t/is @consumer-called?)))))
