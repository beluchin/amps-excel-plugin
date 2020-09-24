(ns amps-excel-plugin.excel.functions-test
  (:require [amps-excel-plugin.excel.functions :as sut]
            [amps-excel-plugin.state :as state]
            [clojure.test :as t]))

(declare array-2d-equals)
(t/deftest java-expand
  (t/testing "existing subscription; no data"
    (with-redefs [state/subscription? (constantly true)
                  state/find-data (constantly nil)]
      (t/is (array-2d-equals
              [["pending"]]
              (sut/java-expand :foo)))))

  (t/testing "invalid subscriptions"
    (with-redefs [state/subscription? (constantly false)]
      (t/is (array-2d-equals 
              [["invalid subscription"]]
              (sut/java-expand :foo))))))

(t/deftest java-unsubscribe
  (t/testing "invalid subscription"
    (t/is (= (sut/java-unsubscribe :invalid-subscription)
             "invalid subscription"))))

(defn- array-2d
  [x]
  (if (= (type x) (type (to-array-2d [[]])))
    x
    (to-array-2d x)))

(defn- array-2d-equals
  [lhs rhs]
  (java.util.Arrays/deepEquals (array-2d lhs) (array-2d rhs)))
