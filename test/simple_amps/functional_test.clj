(ns simple-amps.functional-test
  (:require [simple-amps.functional :as sut]
            [clojure.test :as t]))

(t/deftest state-with-alias-test
  (t/are [state n x state'] (= state' (sut/state-with-alias state n x))
    nil :foo :bar {:name->stream {:foo :bar}}
    {:name->stream {:foo :bar}} :foo :qux {:name->stream {:foo :qux}}))

(t/deftest state-with-qvns-test
  (throw (UnsupportedOperationException.)))

(t/deftest stream-test
  (t/is (= {:uri :foo :topic :bar} (sut/stream :foo :bar nil)))
  (t/is (= {:uri :foo :topic :bar :filter :baz} (sut/stream :foo :bar :baz))))

