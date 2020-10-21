(ns amps-excel-plugin.functional.multi-message)

(declare ok-advance-first?)
(defn merged
  "merges two vectors of map keys - as returned by keys-in"
  [keys1 keys2]
  (loop [prior-k1? nil
         ks1       keys1
         prior-k2? nil
         ks2       keys2
         accum     []]
    (if (every? empty? [ks1 ks2])
      accum
      (let [k1? (first ks1)
            k2? (first ks2)]
        (cond
          (= k1? k2?)
          (recur k1? (next ks1) k2? (next ks2) (conj accum k1?))
          
          (ok-advance-first? prior-k1? k1? prior-k2? k2?)
          (recur k1? (next ks1) prior-k2? ks2 (conj accum k1?))
          
          :else ;; advance second
          (recur prior-k1? ks1 k2? (next ks2) (conj accum k2?)))))))

(defn- ancestor?
  [coll? k]
  (cond
    (nil? coll?)
    false
    
    (< (count k) (count coll?))
    false

    (every? #(= (first %) (second %)) (map vector coll? k))
    true
    
    :else
    false))

(declare parent)
(defn- ok-advance-first?
  "only one of k1? and k2? can be nil"
  [prior-k1? k1? prior-k2? k2?]
  (cond
    (nil? k2?) true
    (nil? k1?) false
    (ancestor? (parent prior-k1?) k1?) true
    :else (not (ancestor? (parent prior-k2?) k2?))))

(defn- parent
  "nil if key is nil or contains only one element"
  [k?]
  (butlast k?))

(comment
  (conj [1 2] 3) ; [1 2 3]
  (rest []) ; ()
  (every? empty? [[] []])
  (vec (butlast [1 2]))
  (map vector [1 2] [3])
  (ancestor? '() [:a])
  (parent [])
  (butlast [1])
  (butlast nil)
  )
