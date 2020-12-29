(ns amps-excel-plugin.functional-test
  (:require [amps-excel-plugin.functional :as sut]
            [cheshire.core :as cheshire]
            [clojure.test :as t]))

(t/deftest cheshire
  (t/testing "array of maps"
    (t/is (= {"a" [{"b" 2} {"c" 3}]}
             (cheshire/parse-string "{\"a\": [{\"b\": 2},{\"c\": 3}]}")))))

(t/deftest value-in-test
  (t/testing "happy path"
    (t/are [m ks1 index-fn ks2 result] (= result (sut/value-in m ks1 index-fn ks2))
      {:a [{:b :result}]} [:a] (constantly 0) [:b] :result))
  (t/testing "reliability"
    ;; - ks1 does not lead to a sequential value
    (t/is (thrown? RuntimeException (sut/value-in {:a 1} [:a] (constantly 0) [])))
    ;; - index-fn returns an index out of bounds
    (t/is (thrown? RuntimeException (sut/value-in {:a [{:b 1}]} [:a] (constantly 42) [])))
    ;; - index-fn does not return a number
    (t/is (thrown? RuntimeException (sut/value-in {:a [{:b 1}]} [:a] (constantly :hello) [])))))

