(ns amps-excel-plugin.functional.multi-message-test
  (:require [amps-excel-plugin.functional.multi-message :as multi-message]
            [clojure.test :as t]))

(t/deftest merged-test
  (t/testing "every empty"
    (t/is (= [] (multi-message/merged [] []))))

  (t/testing "same key"
    (t/are [ks1 ks2 merged] (= merged (multi-message/merged ks1 ks2))
      [[:a]]      [[:a]]      [[:a]]
      [[:a] [:b]] [[:a] [:b]] [[:a] [:b]]))

  (t/testing "different sizes"
    (t/are [ks1 ks2 merged] (= merged (multi-message/merged ks1 ks2))
      []     [[:a]] [[:a]]
      [[:a]] []     [[:a]]))

  (t/testing "different key"
    (t/are [ks1 ks2 merged] (= merged (multi-message/merged ks1 ks2))
      [[:a]] [[:b]] [[:a] [:b]]
      [[:b]] [[:a]] [[:b] [:a]]))

  (t/testing "keep keys with common ancestor together"
    (t/are [ks1 ks2 merged] (= merged (multi-message/merged ks1 ks2))
      [[:a :b1] [:a :b2]] [[:c]]              [[:a :b1] [:a :b2] [:c]]
      [[:c]]              [[:a :b1] [:a :b2]] [[:c] [:a :b1] [:a :b2]]))

  (t/testing "ad-hoc"
    (t/are [ks1 ks2 merged] (= merged (multi-message/merged ks1 ks2))
      [[:a] [:b]]     [[:c :d1] [:c :d2]]      [[:a] [:b] [:c :d1] [:c :d2]]
      [[:a :b1] [:c]] [[:a :b1] [:a :b2] [:d]] [[:a :b1] [:a :b2] [:c] [:d]])))

(t/deftest side-by-side-test
  (t/testing "flat"
    (t/are [m1 m2 rows] (= rows (multi-message/side-by-side m1 m2))
      {"a" 1}       {"a" 2}       [["/a"   1   2  ]]
      {"a" 1}       {}            [["/a"   1   nil]]
      {"a" 1 "b" 2} {"a" 1 "c" 3} [["/a"   1   1  ]
                                   ["/b"   2   nil]
                                   ["/c"   nil 3  ]]
      {"a" {"b" 1}} {}            [["/a/b" 1   nil]]
      {"a" 1}       {"a" {"b" 2}} [["/a"   1   nil]
                                   ["/a/b" nil 2]]))

  (t/testing "nested"
    (t/are [m1 m2 rows] (= rows (multi-message/side-by-side m1 m2))
      {"a" [1]}    {"a" [1]}      [["/a" 1 1]])))

#_(t/deftest closest-match-test

  ;; supports only maps where the leafs are primitives
  
  (t/are [m ms closest-match] (= closest-match (mm/closest-match m ms))
    ;; the trivial case
    {:a 1} [{:a 1}] {:a 1}

    ;; the one with most identical leafs wins
    {:a 1 :b 2 :c 3} [{:a 1 :b 2 :c 4} {:a 1 :b 5 :c 6}] {:a 1 :b 3 :c 4}

    ;; if multiple with same number of identical leafs, the one with
    ;; fewer leafs
    {:a 1 :b 2 :c 3} [{:a 1 :b 2 :c 4} {:a 1 :b 2}] {:a 1 :b 2}

    ;; if multiple with same number of identical leafs / fewer leafs, then random
    {:a 1 :b 3}))

(t/deftest rows-test
  (t/testing "leaf is not a seq"
    (t/is (= [["/:a" 1 2]] (multi-message/rows [:a] {:a 1} {:a 2}))))
  
  (t/testing "leafs are a seq of primitives of the same size"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a [1]}    {:a [2]}    [["/:a" 1 2]]
      [:a] {:a [1 :x]} {:a [2 :y]} [["/:a" 1 2]
                                    ["/:a" :x :y]]))

  (t/testing "leafs are a seq of primitives of different size"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a [1 :x]} {:a [2]} [["/:a" 1 2]
                                 ["/:a" :x nil]]))

  (t/testing "one leaf is a primitive, the other a sequence"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a [1]}    {:a 2} [["/:a" 1 2]]
      [:a] {:a [1 :x]} {:a 2} [["/:a" 1 2]
                               ["/:a" :x nil]]))

  (t/testing "one leaf is a primitive, the other not a leaf - a map"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a 1}      {:a {:b 2}} [["/:a"    1   nil]]
      [:a] {:a {:b 1}} {:a 2}      [["/:a"    nil 2]]))

  (t/testing "leafs are sequences of maps"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a [{:b 1}]} {:a [{:b 2}]} [["/:a/:b" 1 2]]))

  (t/testing "one leaf is seq of maps, the other is a primitive"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a [{:b 1}]} {:a 2} [["/:a/:b" 1   nil]
                                 ["/:a"    nil 2]]))

  #_(t/testing "a seq of primitives vs a map"
    (t/are [leafpath m1 m2 rows] (= rows (multi-message/rows leafpath m1 m2))
      [:a] {:a [1 2]} {:a {:b 2}} [["/:a"    1   nil]
                                   ["/:a"    2   nil]
                                   ["/:a/:b" nil 2]])))
  
