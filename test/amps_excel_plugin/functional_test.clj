(ns amps-excel-plugin.functional-test
  (:require [amps-excel-plugin.functional :as sut]
            [cheshire.core :as cheshire]
            [clojure.test :as t]))

(t/deftest cheshire
  (t/testing "array of maps"
    (t/is (= {"a" [{"b" 2} {"c" 3}]}
             (cheshire/parse-string "{\"a\": [{\"b\": 2},{\"c\": 3}]}")))))

(t/deftest value-in-test
  (t/are [m ks1 index-fn ks2 result] (= result (sut/value-in m ks1 index-fn ks2))
    {:a [{:b 1}]} [:a] (constantly 0) [:b] 1))

