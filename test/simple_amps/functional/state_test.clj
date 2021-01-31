(ns simple-amps.functional.state-test
  (:require [simple-amps.functional.state :as sut]
            [clojure.test :as t]))

(t/deftest state-after-new-alias-test
  (t/are [state n x state'] (= state' (sut/state-after-new-alias state n x))
    nil :foo :bar {:name->sub {:foo :bar}}
    {:name->sub {:foo :bar}} :foo :qux {:name->sub {:foo :qux}}))

(t/deftest state-after-new-qvns-test
  (t/are [state n x state'] (= state' (sut/state-after-new-qvns state n x))
    nil :foo :bar {:name->qvns-set {:foo #{:bar}}}
    {:name->qvns-set {:foo #{:bar}}} :foo :qux {:name->qvns-set {:foo #{:bar :qux}}}))
