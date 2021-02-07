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
  (t/is (= "(f1) AND (f2) AND (f3)" (sut/combine "f1" "f2" "f3"))))

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
  (t/testing "happy path"
    (with-redefs [sut/value        #(when (= %& [:m :qvns]) :x)
                  f-state/qvns-set #(when (= %& [:state :sub]) #{:qvns})
                  sut/in-scope?    #(when (= %& [:m :qvns]) true)]
      (t/is (= [[:x :qvns]] (sut/handle :m :sub :state))))))

(t/deftest in-scope?
  (t/are [m s r] (= r (sut/in-scope? m {:filter (expr/parse-binary-expr s)}))
    {"a" 1} "/a = 1" true
    {"a" 1} "/a = 42" false))

(t/deftest subscribe-action+args-test
  (with-redefs [sut/combine #(when (= [:subf :qvns1f :qvns2f] %&) :f)]
    (t/is (= [:subscribe [{:uri :u :topic :t :filter :subf} :f]]
             (sut/subscribe-action+args :a {:alias->sub
                                            {:a {:uri :u
                                                 :topic :t
                                                 :filter :subf}}

                                            :alias->qvns-set
                                            {:a #{{:filter :qvns1f}
                                                  {:filter :qvns2f}}}})))))

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
