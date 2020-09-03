(ns amps-excel-plugin.core
  (:require [cheshire.core :as json]
            [clojure.string :as string]))

(declare keys-in rows row row-key)

(defn render
  [json]
  (to-array-2d (rows (json/parse-string json))))

(defn rows
  ([m]
   (mapcat #(rows m %) (keys-in m)))

  ([m keys]
   (let [x (get-in m keys)]
     (cond
       (or (number? x)
           (string? x)) [(row keys x)]
       (sequential? x)  (mapcat #(rows {(string/join "/" keys) %}) x)
       :else            (throw (RuntimeException.
                                 (format "%s no supported as value in map"
                                         (.getName (type x)))))))))

(defn- keys-in
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

(defn- row
  [keys primitive]
  [(row-key keys) primitive])

(defn- row-key
  [strings]
  (string/join "/" (cons "" strings)))


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
