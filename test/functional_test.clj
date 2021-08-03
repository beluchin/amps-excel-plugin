(ns functional-test
  (:require [functional :as sut]
            [clojure.test :as t]))

(t/deftest assoc-if-test
  (t/are [k v r] (= r (sut/assoc-if {} k v))
    :k :v {:k :v}
    :k nil {})

  (t/testing "unrelated to presence"
    (t/is (= {:k :v'} (sut/assoc-if {:k :v} :k :v')))))

(t/deftest assoc-if-missing-test
  (t/are [m k v r] (= r (sut/assoc-if-missing m k v))
    {}      :k :v  {:k :v}
    {:k :v} :k :v' {:k :v}))

(t/deftest assoc-in-if-missing-test
  (t/are [m ks v r] (= r (sut/assoc-in-if-missing m ks v))
    {}             [:k1 :k2] :v  {:k1 {:k2 :v}}
    {:k1 {:k2 :v}} [:k1 :k2] :v' {:k1 {:k2 :v}}))

(t/deftest single-test
  (t/is (= :a (sut/single [:a])))
  (t/is (thrown? RuntimeException (sut/single [])))
  (t/is (thrown? RuntimeException (sut/single nil)))
  (t/is (thrown? RuntimeException (sut/single [:a :b]))))
