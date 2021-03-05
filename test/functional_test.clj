(ns functional-test
  (:require [functional :as sut]
            [clojure.test :as t]))

(t/deftest assoc-if-test
  (t/are [k v r] (= r (sut/assoc-if {} k v))
    :k :v {:k :v}
    :k nil {}))

