(ns amps-excel-plugin.core
  (:require [cheshire.core :as json]))

(declare keys-in row)

(defn render [json]
  (let [m (json/parse-string json)]
    (to-array-2d (map #(row m %) (keys-in m)))))

(defn keys-in
  ;; https://stackoverflow.com/a/21769786/614800
  [m]
  (if (map? m)
    (vec 
     (mapcat (fn [[k v]]
               (let [sub (keys-in v)
                     nested (map #(into [k] %) (filter (comp not empty?) sub))]
                 (if (seq nested)
                   nested
                   [[k]])))
             m))
    []))

(defn- row [m keys]
  (let [v (get-in m keys)]
    [(clojure.string/join "/" (cons "" keys)) v]))


(comment
  (type (render (json/generate-string {"a" 1}))) ; [[Ljava.lang.Object;
  (with-out-str (pprint (render (json/generate-string {"a" 1})))) ; "[[\"/a\" 1]]\n"

  (row {"a" 1} ["a"]) ; ["/a" 1]
  (row {"a" 1 "b" {"c" 2}} ["b" "c"]) ; ["/b/c" 2]
 
  (clojure.string/join "/" ["a" "b"])
  (clojure.string/join "/" ["" "a" "b"])

  (keys-in nil) ; []
  (keys-in {}) ; []
  (keys-in {:a 1}) ; [[:a]]
  (keys-in {:a 1 :b {:c 2}}) ; [[:a] [:b :c]]
  (keys-in {"a" 1 "b" {"c" 2}}) ; [["a"] ["b" "c"]]
  )
