(ns amps-excel-plugin.functional-test
  (:require [amps-excel-plugin.functional :as sut]
            [cheshire.core :as cheshire]
            [clojure.test :as t]))

(t/deftest cheshire
  (t/testing "array of maps"
    (t/is (= {"a" [{"b" 2} {"c" 3}]}
             (cheshire/parse-string "{\"a\": [{\"b\": 2},{\"c\": 3}]}")))))

(t/deftest first-kite-test
  (t/testing "happy path"
    (t/is (= {:a {:b 1 :c 2}}
             (sut/first-kite {:a [{:b 1 :c 2} {:b 3}]
                              :d 4}

                             ;; /a/b = 1
                             [[:a :b] 1]))))

  (t/testing "no sequential value - expr is true - the entire map is the kite"
    (t/is (= {:a 1} (sut/first-kite {:a 1} [[:a] 1]))))

  (t/testing "nil"
    (t/are [m expr] (nil? (sut/first-kite m expr))
      ;; no sequential value - expr is not true
      {:a 1} [[:a] 42]

      ;; more than one sequential value
      {:a [{:b [{:c 1}]}]} [[:a :b :c] 1])))

(t/deftest value-test
  (t/testing "happy path"
    (t/is (= 42 (sut/value {:a [{:b 1} {:b 2 :c 42}]}
                           [[:a :b] 2]
                           [:a :c])))))

