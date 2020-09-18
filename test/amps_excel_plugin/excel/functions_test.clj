(ns amps-excel-plugin.excel.functions-test
  (:require [amps-excel-plugin.excel.functions :as sut]
            [clojure.test :as t]))

(t/deftest java-expand
  (t/testing "terminated subscriptions"
    (t/is (java.util.Arrays/deepEquals
            (sut/java-expand "with - unsubscribed - anywhere")
            (to-array-2d [["**unsubscribed**"]])))))

(t/deftest java-unsubscribe
  (t/testing "terminated subscriptions"
    (t/is (= (sut/java-unsubscribe "with - unsubscribed - anywhere")
             "OK"))))
