(ns amps-excel-plugin.functional)

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

(defn value-in
  "on m, the ks1 lead to a sequence of maps. A map from the sequence
  is selected using the index-fn and from there the ks2 are used
  to get to a value. The index-fn takes a sequence and returns an index"
  [m ks1 index-fn ks2]
  (-> m
      (get-in ks1)
      (#(nth % (index-fn %)))
      (get-in ks2)))
