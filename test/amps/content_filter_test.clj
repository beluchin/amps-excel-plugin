(ns amps.content-filter-test
  (:require andor
            [amps.content-filter :as sut]
            [clojure.test :as t]))

(t/deftest remove-test
  (t/testing "remove if added (or'ed) previously"
    (t/is (= :a (sut/remove (sut/add :a :b) :b)))
    (t/is (= (andor/or :a :b) (sut/remove (andor/add :a :b) :c))))
  
  (t/testing "remove if identical"
    (t/is (nil? (sut/remove :a :a))))
  
  (t/testing "otherwise is unsupported"
    (t/is (thrown? UnsupportedOperationException (sut/remove :a :b)))))

