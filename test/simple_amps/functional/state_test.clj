(ns simple-amps.functional.state-test
  (:require [simple-amps.functional.state :as sut]
            [clojure.test :as t]))

(t/deftest qvns-set-test
  (t/testing "from subscription alias"
    (t/is (= :foo (sut/qvns-set {:alias->qvns-set {:a :foo}} :a))))

  (t/testing "from subscription"
    (t/are [state sub qvns-set] (= qvns-set (sut/qvns-set state sub))
      {:alias->qvns-set {:a :foo-set} :alias->sub {:a {:bar :qux}}}
      {:bar :qux}
      :foo-set
      
      ;; a subscription has multiple aliases.
      ;; https://stackoverflow.com/a/42771807/614800
      )))

(t/deftest state-after-new-alias-test
  (t/are [state a x state'] (= state' (sut/state-after-new-alias state a x))
    nil :foo :bar {:alias->sub {:foo :bar}}
    {:alias->sub {:foo :bar}} :foo :qux {:alias->sub {:foo :qux}}))

(t/deftest state-after-new-ampsies-test
  (t/are [s sub a s'] (= s' (sut/state-after-new-ampsies s sub a))
    nil :sub :a {:sub->ampsies {:sub :a}}
    {:sub->ampsies {:sub :a}} :sub :a' {:sub->ampsies {:sub :a'}}))

(t/deftest state-after-new-executor-if-absent-test
  (t/are [s u e s'] (= s' (sut/state-after-new-executor-if-absent s u e))
    nil :u :e {:uri->executor {:u :e}}
    {:uri->executor {:u :e}} :u :e' {:uri->executor {:u :e}}))

(t/deftest state-after-new-qvns-test
  (t/are [state a x state'] (= state' (sut/state-after-new-qvns state a x))
    nil :foo :bar {:alias->qvns-set {:foo #{:bar}}}
    {:alias->qvns-set {:foo #{:bar}}} :foo :qux {:alias->qvns-set {:foo #{:bar :qux}}}))
