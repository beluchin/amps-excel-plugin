(ns amps.expr-test
  (:require [amps.expr :as sut]
            andor
            [clojure.test :as t]))

(extend-protocol sut/StringForm
  clojure.lang.Keyword
  (string-form [this] (name this)))

(t/deftest string-form-test
  (t/testing "and"
    (t/is (= "(b) AND (a)" (sut/string-form (andor/and :a :b)))))

  (t/testing "or"
    (t/is (= "(b) OR (a)" (sut/string-form (andor/or :a :b)))))

  (t/testing "nested"
    (t/is (= "((c) AND (d)) OR ((b) AND (a))"
             (sut/string-form (andor/or (andor/and :a :b)
                                        (andor/and :c :d)))))
    (t/is (= "((c) OR (d)) AND ((b) OR (a))"
             (sut/string-form (andor/and (andor/or :a :b)
                                         (andor/or :c :d)))))))
