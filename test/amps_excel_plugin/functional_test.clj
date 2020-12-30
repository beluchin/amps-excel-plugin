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

  (t/testing "more than one kite available"
    (throw (UnsupportedOperationException.)))

  (t/testing "expr is not boolean - kite contains a value for the expr"
    (throw (UnsupportedOperationException.))))

(t/deftest cheshire
  (t/testing "array of maps"
    (t/is (= {"a" [{"b" 2} {"c" 3}]}
             (cheshire/parse-string "{\"a\": [{\"b\": 2},{\"c\": 3}]}")))))


