(ns simple-amps.functional.state-test
  (:require [simple-amps.functional.state :as sut]
            [clojure.test :as t]))

(t/deftest qvns-set-test
  #_(t/testing "from AMPS client"
    (t/is (= #{:qvns} (sut/qvns-set {:uri->client {:u :c}
                                     :alias->sub {:a {:uri :u}}
                                     :alias->qvns-set {:a #{:qvns}}}
                                    :c))))

  (t/testing "from subscription alias"
    (t/is (= :foo-set (sut/qvns-set {:alias->qvns-set {"a" :foo-set}} "a"))))

  (t/testing "from subscription"
    (t/are [state sub qvns-set] (= qvns-set (sut/qvns-set state sub))
      {:alias->qvns-set {:a :foo-set}
       :alias->sub {:a {:bar :qux}}} {:bar :qux} :foo-set
      
      ;; a subscription has multiple aliases.
      ;; https://stackoverflow.com/a/42771807/614800
      )))

(t/deftest state-after-delete-many-test
  (t/testing "uri->client"
    (t/is (= {:uri->client {"u1" :c}} (sut/state-after-delete-many
                                        {:uri->client {"u1" :c, "u2" :c}}
                                        [["u2" :c]]))))

  (t/testing "sub->ampsies"
    (t/is (= {:sub->ampsies {{:k :v1} :ampsies}}
             (sut/state-after-delete-many
               {:sub->ampsies {{:k :v1} :ampsies
                               {:k :v2} :ampsies}}
               [[{:k :v2} :ampsies]])))))

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
