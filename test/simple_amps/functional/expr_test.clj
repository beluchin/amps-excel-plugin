(ns simple-amps.functional.expr-test
  (:require [simple-amps.functional.expr :as sut]
            [clojure.test :as t]))

(t/deftest binary-expr-test
  (t/is (= [:a] (sut/common-path (sut/->BinaryExpr (sut/->ValueExpr [:a])
                                                   =
                                                   (sut/->ConstantExpr 42)))))
  (t/are [m ks x result] (= result (sut/evaluate
                                     (sut/->BinaryExpr (sut/->ValueExpr ks)
                                                       =
                                                       (sut/->ConstantExpr x))
                                     m))
    {:a {:b 42}}   [:a :b] 42   true
    {:a {:b :nah}} [:a :b] 42   false
    {:a 1}         [:b]    2    false))

(t/deftest constant-expr-test
  (t/is (= nil (sut/common-path (sut/->ConstantExpr 42))))
  (t/is (= 42 (sut/evaluate (sut/->ConstantExpr 42) :whatever))))

(t/deftest parse-binary-expr-test
  (t/testing "happy path"
    (t/are [s kcoll const] (= (sut/->BinaryExpr (sut/->ValueExpr kcoll)
                                                =
                                                (sut/->ConstantExpr const))
                              (sut/parse-binary-expr s))
      "    /a/b  =    42    "  ["a" "b"] 42
      "/a/b  = 'hello'"        ["a" "b"] "hello"
      "/_with_underscores = 1" ["_with_underscores"] 1 )))

(t/deftest parse-constant-expr-test
  (t/are [s value] (= (sut/->ConstantExpr value) (sut/parse-constant-expr s))
    "42" 42
    "\"42\"" "42"
    "'42'" "42"))

(t/deftest parse-value-expr-test
  (t/testing "happy path"
    (t/are [s token-coll] (= (sut/->ValueExpr token-coll) (sut/parse-value-expr s))
      "/a/b" ["a" "b"]
      "/_with_underscores" ["_with_underscores"]))
  
  (t/testing "invalid"
    (t/are [s] (nil? (sut/parse-value-expr s))
      "/"
      "/ a"
      "/a = 3")))

(t/deftest value-expr-test
  (t/is (= [:a] (sut/common-path (sut/->ValueExpr [:a]))))
  (t/are [m ks result] (= result (sut/evaluate (sut/->ValueExpr ks) m))
    {:a {:b 42}} [:a :b] 42
    {}           [:a]    nil))
