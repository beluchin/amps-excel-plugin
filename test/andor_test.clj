(ns andor-test
  (:require [andor :as sut]
            [clojure.test :as t]))

(t/deftest and-test
  (t/testing "collapse nil"
    (t/is (= :x (sut/and :x nil)))
    (t/is (= :x (sut/and nil :x))))

  (t/testing "commutative"
    (t/is (= (sut/and :x :y) (sut/and :y :x))))

  (t/testing "dedup"
    (t/is (= :x (sut/and :x :x)))))

(t/deftest optimize-test
  (t/testing "or of ands with common operand"
    (t/is (= (sut/and :x (sut/or :y :z))
             (sut/optimize (sut/or (sut/and :x :y)
                                   (sut/and :x :z)))))))

(t/deftest or-test
    (t/testing "collapse nil"
    (t/is (= :x (sut/or :x nil)))
    (t/is (= :x (sut/or nil :x))))

  (t/testing "commutative"
    (t/is (= (sut/or :x :y) (sut/or :y :x))))

  (t/testing "dedup"
    (t/is (= :x (sut/or :x :x)))))
