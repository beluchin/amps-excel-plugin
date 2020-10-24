(ns amps-excel-plugin.functional.multi-message-test
  (:require [amps-excel-plugin.functional.multi-message :as multi-message]
            [clojure.test :as t]))

(t/deftest merged
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

(t/deftest side-by-side
  (t/are [m1 m2 rows] (= rows (multi-message/side-by-side m1 m2))
      {"a" 1}       {"a" 2}       [["/a"   1   2]]
      {"a" 1}       {}            [["/a"   1   nil]]
      {"a" 1 "b" 2} {"a" 1 "c" 3} [["/a"   1   1]
                                   ["/b"   2   nil]
                                   ["/c"   nil 3]]
      {"a" {"b" 1}} {}            [["/a/b" 1   nil]]))
