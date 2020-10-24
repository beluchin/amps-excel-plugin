(ns amps-excel-plugin.functional-test
  (:require [amps-excel-plugin.functional :as sut]
            [cheshire.core :as cheshire]
            [clojure.test :as t]))

(t/deftest cheshire
  (t/testing "array of maps"
    (t/is (= {"a" [{"b" 2} {"c" 3}]}
             (cheshire/parse-string "{\"a\": [{\"b\": 2},{\"c\": 3}]}")))))

