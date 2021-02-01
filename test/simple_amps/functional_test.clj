(ns simple-amps.functional-test
  (:require [simple-amps.functional :as sut]
            [clojure.test :as t]))

(t/deftest combine-test
  (t/is (= "(f1) AND (f2) AND (f3)" (sut/combine "f1" "f2" "f3"))))

(t/deftest subscribe-action+args-test
  (with-redefs [sut/combine #(when (= [:subf :qvns1f :qvns2f] %&) :f)]
    (t/is (= [:subscribe [{:uri :u :topic :t :filter :subf} :f]]
             (sut/subscribe-action+args :n {:name->sub
                                            {:n {:uri :u
                                                 :topic :t
                                                 :filter :subf}}

                                            :name->qvns-set
                                            {:n #{{:filter :qvns1f}
                                                  {:filter :qvns2f}}}

                                            ;; no sub-id for name
                                            }))))

  #_(t/testing "replace-filter"
      (throw (UnsupportedOperationException.)))

  #_(t/testing "unsubscribe"
      (throw (UnsupportedOperationException.))))

(t/deftest subscription-test
  (t/is (= {:uri :foo :topic :bar} (sut/subscription :foo :bar nil)))
  (t/is (= {:uri :foo :topic :bar :filter :baz} (sut/subscription :foo :bar :baz))))

