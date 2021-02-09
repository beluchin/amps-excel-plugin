(ns amps-excel-plugin.excel.functions-test
  (:require [amps-excel-plugin.excel.functions :as sut]
            [amps-excel-plugin.state :as state]
            [clojure.test :as t]))

(declare array-2d-equals)
#_(t/deftest java-expand
  (t/testing "existing subscription; no data"
    (with-redefs [state/try-get (constantly {})]
      (t/is (array-2d-equals
              [["pending"]]
              (sut/java-expand :foo)))))

  (t/testing "invalid subscriptions"
    (with-redefs [state/try-get (constantly nil)]
      (t/is (array-2d-equals 
              [["invalid subscription"]]
              (sut/java-expand :foo))))))

(t/deftest java-unsubscribe
  (t/testing "invalid subscription"
    (t/is (= "invalid subscription"
             (sut/java-unsubscribe "something invalid"))))

  (t/testing "**unsubscribed**"
    (t/is (= "OK"
             (sut/java-unsubscribe "**unsubscribed** whatever")))))

(defn- array-2d
  [x]
  (if (= (type x) (type (to-array-2d [[]])))
    x
    (to-array-2d x)))

(defn- array-2d-equals
  [lhs rhs]
  (java.util.Arrays/deepEquals (array-2d lhs) (array-2d rhs)))
