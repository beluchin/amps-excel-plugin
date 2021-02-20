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
    ["f1" "f2" "f3"] "(f1) AND (f2) AND (f3)"
    [nil "f1" "f2"] "(f1) AND (f2)"
    [nil] nil
    ["f1"] "(f1)"))

(declare binary-expr)
(t/deftest first-kite-test
  (t/testing "happy path"
    (t/is (= {:a {:b 1 :c 2}}
             (sut/first-kite {:a [{:b 1 :c 2} {:b 3}]
                              :d 4}

                             ;; /a/b = 1
                             (binary-expr [:a :b] = 1)))))

  (t/testing "no sequential value - expr is true - the entire map is the kite"
    (t/is (= {:a 1} (sut/first-kite {:a 1} (binary-expr [:a] = 1)))))

  (t/testing "nil"
    (t/are [m expr] (nil? (sut/first-kite m (apply binary-expr expr)))
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
    {"a" 1} "/a = 42" false))

(t/deftest qvns-coll-test
  (t/is (= [:qvns1 :qvns2] (sut/qvns-coll {:alias->sub {:a1 {:uri :u}
                                                        :a2 {:uri :u}}
                                           :alias->qvns-set {:a1 #{:qvns1}
                                                             :a2 #{:qvns2}}}
                                          :u))))

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

(t/deftest revisit-test
  (t/testing "subscription and qvns in place; subscription is inactive"
    (with-redefs [sut/subscribe-action+args (fn [alias _] (when (= alias "a")
                                                            [:action :args]))]
      (t/is (= [:action :args] (sut/revisit "a" {:alias->sub {"a" :sub}
                                                 :alias->qvns-set {"a" :qvns-set}
                                                 :sub->ampsies {}})))))

  (t/testing "subscription in place; no qvns"
    (t/is (nil? (sut/revisit "a" {:alias->sub {"a" :sub}
                                  :alias->qvns-set {}}))))

  (t/testing "no subscription in place"
    (t/is (nil? (sut/revisit "a" {:alias->sub {}})))))

(t/deftest state-after-delete-test
  (t/testing "deleting client"
    (t/is (= {:uri->client {"u2" :c2}, :sub->ampsies {{:k :v2} {:client :c2}}}
             (sut/state-after-delete {:uri->client {"u1" :c1, "u2" :c2}
                                      :sub->ampsies {{:k :v1} {:client :c1}
                                                     {:k :v2} {:client :c2}}}
                                     :c1)))))

(t/deftest subscribe-action+args-test
  (with-redefs [sut/combine #(when (= [:subf :qvns1f :qvns2f] %&) :f)]
    (t/is (= [:subscribe [{:uri :u :topic :t :filter :subf} :f]]
             (sut/subscribe-action+args "a" {:alias->sub
                                            {"a" {:uri :u
                                                 :topic :t
                                                 :filter :subf}}

                                            :alias->qvns-set
                                            {"a" #{{:filter+expr [:qvns1f :foo]}
                                                  {:filter+expr [:qvns2f :foo]}}}})))))

(t/deftest subscription-test
  (t/is (= {:uri :foo :topic :bar} (sut/subscription :foo :bar nil)))
  (t/is (= {:uri :foo :topic :bar :filter :baz} (sut/subscription :foo :bar :baz))))

(declare value-expr)
(t/deftest value-test
  (t/testing "happy path"
    (t/is (= 42 (sut/value {:a [{:b 1} {:b 2 :c 42}]}
                           (binary-expr [:a :b] = 2)
                           (value-expr [:a :c]))))))

(defn- binary-expr
  [ks op const]
  (expr/->BinaryExpr (expr/->ValueExpr ks) op (expr/->ConstantExpr const)))

(defn- value-expr
  [ks]
  (expr/->ValueExpr ks))
