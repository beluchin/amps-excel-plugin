(ns amps-excel-plugin.functional)

(defn leafpaths
  ;; https://stackoverflow.com/a/21769786/614800
  "a leafpath is a sequence of keys from the top a value that is not a map"
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
