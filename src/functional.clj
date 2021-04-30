(ns functional)

(defn assoc-if
  "Same as assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> kvs
    (partition 2)
    (filter second)
    (map vec)
    (into m)))

(defn assoc-if-missing [m k v]
  (if (m k) m (assoc m k v)))

(defn assoc-in-if-missing [m ks v]
  (if (get-in m ks) m (assoc-in m ks v)))

(defn common-prefix
  "(... [:a :b] [:a :c]) => [:a]"
  [coll1 coll2]
  {:pre [(nil? coll2)]}
  coll1)

(defn index-of-first
  "https://stackoverflow.com/a/51145673/614800"
  [pred coll]
  (ffirst (filter (comp pred second) (map-indexed list coll))))

(defn leafpaths
  ;; https://stackoverflow.com/a/21769786/614800
  "a leafpath is a sequence of keys from the top to a value that is not a map"
  [m]
  (if (map? m)
    (vec 
     (mapcat (fn [[k v]]
               (let [sub (leafpaths v)
                     nested (map #(into [k] %) (filter (comp not empty?) sub))]
                 (if (seq nested)
                   nested
                   [[k]])))
             m))
    []))
