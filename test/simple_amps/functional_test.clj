(ns simple-amps.functional-test
  (:require [cheshire.core :as cheshire]
            [clojure.test :as t]
            [simple-amps.functional :as sut]
            [simple-amps.functional.expr :as expr]
            [simple-amps.functional.state :as f-state]))

(t/deftest cheshire-test
  (t/testing "array of maps"
    (t/is (= {"a" [{"b" 2} {"c" 3}]}
             (cheshire/parse-string "{\"a\": [{\"b\": 2},{\"c\": 3}]}")))))

(t/deftest combine-test
  (t/are [args r] (= r (apply sut/combine args))
    ["f1" ["f2" "f3"]] "(f1) AND ((f2) OR (f3))"
    ["f1" ["f2"]]      "(f1) AND (f2)"
    [nil  ["f2"]]      "f2"
    [nil  ["f2" "f3"]] "(f2) OR (f3)"
    ["f1" nil]         "f1")

  (t/testing "one qvns without filter removes the filter on the subscription"
    (t/is (= "f1" (sut/combine "f1" [nil "f2"])))))

(t/deftest dedup-test
  (t/are [and-filter or-filter-coll r] (= r (sut/dedup and-filter or-filter-coll))
    :andf [:orf1 :orf1] [:andf #{:orf1}]
    :f    [:f]          [:f    #{}]
    nil   [:orf1 :orf1] [nil   #{:orf1}]
    :f    nil           [:f    #{}]
    nil   nil           [nil   #{}]))

(declare binary-expr)
(t/deftest first-kite-test
  (t/testing "happy path"
    (t/is (= {:a {:b 1 :c 2}}
             (sut/first-nested-map {:a [{:b 1 :c 2} {:b 3}]
                              :d 4}

                             ;; /a/b = 1
                             (binary-expr [:a :b] = 1)))))

  (t/testing "no sequential value - expr is true - the entire map is the kite"
    (t/is (= {:a 1} (sut/first-nested-map {:a 1} (binary-expr [:a] = 1)))))

  (t/testing "nil"
    (t/are [m expr] (nil? (sut/first-nested-map m (apply binary-expr expr)))
      ;; no sequential value - expr is not true
      {:a 1} [[:a] = 42]

      ;; more than one sequential value
      {:a [{:b [{:c 1}]}]} [[:a :b :c] = 1])))

(t/deftest handle-test
  (t/testing "with qvns in scope"
    (with-redefs [sut/qvns-set #(when (= %& [:state :sub]) #{:qvns})]
      (t/testing "happy path"
        (with-redefs [sut/in-scope? #(when (= %& [:m :qvns]) true)
                      sut/value      #(when (= %& [:m :qvns]) :x)]
          (t/is (= [[:x :qvns]] (sut/handle :m :sub :state)))))

      (t/testing "not in scope"
        (with-redefs [sut/in-scope? #(when (= %& [:m :qvns]) false)]
          (t/is (empty? (sut/handle :m :sub :state)))))))

  (t/testing "no qvns in scope"
    (with-redefs [sut/qvns-set (constantly nil)]
      (t/is (empty? (sut/handle :m :sub :state))))))

(t/deftest in-scope?-test
  (t/are [m s r] (= r (sut/in-scope?
                        m
                        {:filter+expr [:foo (expr/parse-binary-expr s)]}))
    {"a" 1} "/a = 1" true
    {"a" 1} "/a = 42" false
    {"a" "hello"} "/a = 'hello'" true)

  (t/testing "missing filter"
    (t/is (sut/in-scope? :m {}))))

(t/deftest qvns-coll-test
  (t/is (= [:qvns1 :qvns2] (sut/qvns-set {:alias->sub {:a1 {:uri :u}
                                                        :a2 {:uri :u}}
                                           :alias->qvns-set {:a1 #{:qvns1}
                                                             :a2 #{:qvns2}}}
                                          :u))))

(t/deftest qvns-or-error-test
  (with-redefs [expr/parse-binary-expr (constantly :parsed)
                expr/parse-value-expr (constantly :parsed)]
    (t/are [fi nested-map-expr value-expr consumer qvns] (= qvns
                                                            (sut/qvns-or-error
                                                              fi
                                                              nested-map-expr
                                                              value-expr
                                                              consumer))
      :fi :nested-e :value-e :c {:filter+expr [:fi :parsed]
                                 :nested-map-expr :parsed
                                 :value-expr :parsed
                                 :consumer :c}
      nil nil :value-e :c {:value-expr :parsed, :consumer :c}
      )))

(t/deftest qvns-set-test
  #_(t/testing "from subscription alias"
    (t/is (= :foo-set (sut/qvns-set {:alias->qvns-set {"a" :foo-set}} "a"))))

  (t/testing "from subscription"
    (t/are [state sub qvns-set] (= qvns-set (sut/qvns-set state sub))
      {:alias->qvns-set {:a :foo-set}
       :alias->sub {:a {:bar :qux}}} {:bar :qux} :foo-set
      
      ;; a subscription has multiple aliases.
      ;; https://stackoverflow.com/a/42771807/614800
      )))

(t/deftest resubscribe-args-test
  (with-redefs [sut/combine #(when (= [:subf #{:qvns2f :qvns1f}] %&) :f)]
    (t/is (= [{:uri :u :topic :t :filter :subf}
              :f
              #{{:filter+expr [:qvns1f :foo]}
                 {:filter+expr [:qvns2f :foo]}}
              #{{:filter+expr [:qvns2f :foo]}}
              :ampsies]

             (sut/resubscribe-args
               {:uri :u :topic :t :filter :subf}
               #{{:filter+expr [:qvns1f :foo]}
                 {:filter+expr [:qvns2f :foo]}}
               #{{:filter+expr [:qvns1f :foo]}}
               :ampsies)))))

(t/deftest subscription-action+args-test
  (t/testing "while not connected to amps"
    (with-redefs [sut/subscribe-args #(when (= [:sub :qvns-set] %&) :args)]
      (t/is (= [:subscribe :args] (sut/subscription-action+args
                                    :a
                                    {:alias->sub {:a :sub}
                                     :alias->qvns-set {:a :qvns-set}
                                     :sub->ampsies {}})))))

  (t/testing "while connected to amps"
    (with-redefs [sut/resubscribe-args #(when (= [:sub
                                                  :qvns-set
                                                  :activated-qvns-set
                                                  :ampsies]
                                                 %&)
                                          :args)]
      (t/is (= [:resubscribe :args]
               (sut/subscription-action+args
                 :a
                 {:alias->sub {:a :sub}
                  :alias->qvns-set {:a :qvns-set}
                  :sub->ampsies {:sub :ampsies}
                  :sub->activated-qvns-set {:sub :activated-qvns-set}})))))

  (t/testing "subscription in place; no qvns"
    (t/is (nil? (sut/subscription-action+args "a" {:alias->sub {"a" :sub}
                                                   :alias->qvns-set {}}))))
  (t/testing "no subscription in place"
    (t/is (nil? (sut/subscription-action+args "a" {:alias->sub {}})))))

(t/deftest state-after-delete-test
  (t/testing "deleting client"
    (t/is (= {:uri->client {:u2 :c2}
              :sub->ampsies {:sub2 {:client :c2}}
              :sub->activated-qvns-set {:sub2 :qvns-set2}}
             (sut/state-after-remove-client {:uri->client {:u1 :c1, :u2 :c2}
                                      :sub->ampsies {:sub1 {:client :c1}
                                                     :sub2 {:client :c2}}
                                      :sub->activated-qvns-set {:sub1 :qvns-set1
                                                                :sub2 :qvns-set2}}
                                     :c1)))))

(t/deftest subscribe-args-test
  (with-redefs [sut/combine #(when (= [:subf #{:qvns2f :qvns1f}] %&) :f)]
    (t/is (= [{:uri :u :topic :t :filter :subf}
              :f
              #{{:filter+expr [:qvns1f :foo]}
                {:filter+expr [:qvns2f :foo]}}]
             (sut/subscribe-args
               {:uri :u :topic :t :filter :subf}
               #{{:filter+expr [:qvns1f :foo]}
                 {:filter+expr [:qvns2f :foo]}})))))

(t/deftest subscription-test
  (t/is (= {:uri :foo :topic :bar} (sut/subscription :foo :bar nil)))
  (t/is (= {:uri :foo :topic :bar :filter :baz} (sut/subscription :foo :bar :baz))))

(declare value-expr)
(t/deftest value-test
  (t/is (= 42 (sut/value {:a [{:b 1} {:b 2 :c 42}]}
                         (binary-expr [:a :b] = 2)
                         (value-expr [:a :c]))))
  (t/testing "missing nested context expr"
    (t/is (= 42 (sut/value {:a {:b 42}}
                           nil
                           (value-expr [:a :b]))))))

(defn- binary-expr
  [ks op const]
  (expr/->BinaryExpr (expr/->ValueExpr ks) op (expr/->ConstantExpr const)))

(defn- value-expr
  [ks]
  (expr/->ValueExpr ks))
