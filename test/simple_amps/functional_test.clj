(ns simple-amps.functional-test
  (:require [simple-amps.functional :as sut]
            [clojure.test :as t]))

(t/deftest revisit-test
  (t/testing "subscribe"
    (with-redefs [sut/combine #(when (= [:subf :qvnsf] %&) :f)]
      (t/is (= ([:subscribe [:u :t :f :s]])
               (sut/revisit :n {:name->subscription {:n {:uri :u
                                                         :topic :t
                                                         :filter :subf}}
                                :name->qvns {:n {:filter :qvnsf}}
                                ;; no sub-id for name
                                })))))

  (t/testing "replace-filter"
    (throw (UnsupportedOperationException.)))

  (t/testing "unsubscribe"
    (throw (UnsupportedOperationException.))))

(t/deftest state-after-new-alias-test
  (t/are [state n x state'] (= state' (sut/state-after-new-alias state n x))
    nil :foo :bar {:name->sub {:foo :bar}}
    {:name->sub {:foo :bar}} :foo :qux {:name->sub {:foo :qux}}))

(t/deftest state-after-new-qvns-test
  (t/are [state n x state'] (= state' (sut/state-after-new-qvns state n x))
    nil :foo :bar {:name->qvns-set {:foo #{:bar}}}
    {:name->qvns-set {:foo #{:bar}}} :foo :qux {:name->qvns-set {:foo #{:bar :qux}}}))

(t/deftest subscription-test
  (t/is (= {:uri :foo :topic :bar} (sut/subscription :foo :bar nil)))
  (t/is (= {:uri :foo :topic :bar :filter :baz} (sut/subscription :foo :bar :baz))))

