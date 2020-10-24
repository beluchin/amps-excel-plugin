(ns amps-excel-plugin.functional)

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
