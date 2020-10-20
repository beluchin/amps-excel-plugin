(ns amps-excel-plugin.core-test
  (:require [amps-excel-plugin.core :as core]
            [cheshire.core :as json]
            [clojure.test :refer :all]))

(declare replace-longs-with-ints)

(deftest render
  (is (java.util.Arrays/deepEquals
        (core/render (json/generate-string {"a" 1}))
        (replace-longs-with-ints (to-array-2d [["/a" 1]])))))

(deftest json-of-primitives-no-lists
  (are [m rows] (= (core/rows m) rows)
    {"a" 1}              [["/a" 1]]
    {"a" 1, "b" {"c" 2}} [["/a" 1] ["/b/c" 2]]))

(deftest lists
  (are [m rows] (= (core/rows m) rows)
    {"a" [1 2]} [["/a" 1] ["/a" 2]]))

(deftest preserve-the-order
  (are [m rows] (= (core/rows m) rows)
    {"a" 1, "b" 2} [["/a" 1] ["/b" 2]]
    {"b" 2, "a" 1} [["/b" 2] ["/a" 1]]))

(defn- replace-longs-with-ints 
  "replaces longs for ints on the input two-dim Java array"
  [arr]
  (doseq [x (range 0 (alength arr))
          y [0 1]]
    (let [e (aget arr x y)]
      #_(println (type e))
      (when (= Long (type e))
        (aset arr x y (int e)))))
  arr)

(comment
  (alength (to-array []))
  (alength (to-array-2d [[]]))
  (t! (to-array-2d [["/a" 1] ["/b/c" 2]]))
  (type (t! (to-array-2d [["/a" 1] ["/b/c" 2]])))
  (range 0 1)
  (= Long (type 1))
  (aget (to-array-2d [["/a" 1] ["/b/c" 2]]) 1 1) 
  (type (to-array-2d [["/a" 1]])) ; [[Ljava.lang.Object;
  (= (to-array-2d [["/a" 1]]) (to-array-2d [["/a" 1]])) ; false
  (java.util.Arrays/deepEquals (to-array-2d [["/a" 1]]) (to-array-2d [["/a" 1]])) ; true
  (java.util.Arrays/deepEquals (to-array ["a" 1]) (to-array ["a" 1])) ; true
  )
