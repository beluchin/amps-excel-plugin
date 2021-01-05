(ns functional)

(defn common-prefix
  "(... [:a :b] [:a :c]) => [:a]"
  [coll1 coll2]
  {:pre [(nil? coll2)]}
  coll1)

(defn index-of-first
  "https://stackoverflow.com/a/51145673/614800"
  [pred coll]
  (ffirst (filter (comp pred second) (map-indexed list coll))))
