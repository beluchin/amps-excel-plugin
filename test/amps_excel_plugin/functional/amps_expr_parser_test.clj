(ns amps-excel-plugin.functional.amps-expr-parser-test
  (:require [clojure.test :as t]
            [amps-excel-plugin.functional.amps-expr-parser :as sut]))

(t/deftest parse-subtree-selector-test
  (t/testing "happy path"
    (let [[ks index-fn] (sut/parse-subtree-selector {"a" [:whatever]}
                                                    [["a" "b"] 1])]
      (t/is (and (= ["a"] ks) (= 0 (index-fn [{"b" 1}]))))))
  (t/testing "supports single and double quotes for strings"
    (throw (UnsupportedOperationException.)))
  (t/testing "reliability - no sequential value along the way"
    (throw (UnsupportedOperationException.)))
  (t/testing "reliability - more than one sequential value"
    (throw (UnsupportedOperationException.))))

(t/deftest parse-primitive-expression
  (throw (UnsupportedOperationException.)))
