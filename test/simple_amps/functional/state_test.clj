(ns simple-amps.functional.state-test
  (:require [simple-amps.functional.state :as sut]
            [clojure.test :as t]))

(t/deftest qvns-set-test
  (t/is (= :foo-set (sut/qvns-set {:alias->qvns-set {"a" :foo-set}} "a"))))

(t/deftest state-after-delete-many-test
  (t/testing "uri->client"
    (t/is (= {:uri->client {:u1 :c}} (sut/after-remove-uri->client
                                       {:uri->client {:u1 :c, :u2 :c}}
                                       [:u2]))))

  (t/testing "sub->ampsies"
    (t/is (= {:sub->ampsies {{:k :v1} :ampsies}}
             (sut/after-remove-sub->ampsies
               {:sub->ampsies {{:k :v1} :ampsies
                               {:k :v2} :ampsies}}
               [{:k :v2}])))))

(t/deftest state-after-new-alias-test
  (t/are [state a x state'] (= state' (sut/state-after-new-alias state a x))
    nil :foo :bar {:alias->sub {:foo :bar}}
    {:alias->sub {:foo :bar}} :foo :qux {:alias->sub {:foo :qux}}))

(t/deftest state-after-new-ampsies-test
  (t/are [s sub a s'] (= s' (sut/state-after-new-ampsies s sub a))
    nil :sub :a {:sub->ampsies {:sub :a}}
    {:sub->ampsies {:sub :a}} :sub :a' {:sub->ampsies {:sub :a'}}))

(t/deftest state-after-new-executor-if-absent-test
  (t/are [s u e s'] (= s' (sut/after-new-executor-if-absent s u e))
    nil :u :e {:uri->executor {:u :e}}
    {:uri->executor {:u :e}} :u :e' {:uri->executor {:u :e}}))

