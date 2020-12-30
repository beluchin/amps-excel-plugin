(ns amps-excel-plugin.functional-test
  (:require [amps-excel-plugin.functional :as sut]
            [cheshire.core :as cheshire]
            [clojure.test :as t]))

(t/deftest kite-test
  (t/testing "happy path"
    (t/is (= {:a {:b 1 :c 2}}
             (sut/kite {:a [{:b 1 :c 2} {:b 3}]
                        :d 4}
                       [[:a :b] 1]))))

  (t/testing "expr does not reference a sequential value"
    (throw (UnsupportedOperationException.)))

  (t/testing "expr references more than one sequential value"
    (throw (UnsupportedOperationException.)))

  (t/testing "more than one spoon available"
    (throw (UnsupportedOperationException.)))

  (t/testing "expr is not boolean - spoon contains a value for the expr"
    (throw (UnsupportedOperationException.))))

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

