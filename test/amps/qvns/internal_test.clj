(ns amps.qvns.internal-test
  (:refer-clojure :exclude [ensure])
  (:require [amps.qvns.internal :as sut]
            [clojure.test :as t]))

(def ^:private action sut/action)

(defn- ensure [mgr]
  (throw (UnsupportedOperationException.)))

(defn- initial-subscription []
  (throw (UnsupportedOperationException.)))

(t/deftest ensure-test
  (t/testing "initial subscription"
    (t/is (= (initial-subscription) (-> nil
                                        ensure
                                        action)))))

