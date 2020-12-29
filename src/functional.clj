(ns functional)

(defn index-of-first
  "https://stackoverflow.com/a/51145673/614800"
  [pred coll]
  (ffirst (filter (comp pred second) (map-indexed list coll))))
