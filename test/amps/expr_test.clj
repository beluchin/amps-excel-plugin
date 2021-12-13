(ns amps.expr-test
  (:require [amps.expr :as sut]
            andor
            [clojure.test :as t]))

(extend-protocol sut/StringForm
  clojure.lang.Keyword
  (string-form [this] (name this)))

(t/deftest string-form-test
  (t/testing "and"
    (t/is (= "(:a) AND (:b)" (sut/string-form (andor/and :a :b))))))
