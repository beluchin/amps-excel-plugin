(ns simple-amps.functional.state
  (:refer-clojure :exclude [name]))

(defn qvns-coll
  [state name]
  (-> state :name->qvns-set (get name)))

(defn state-after-new-alias
  [state name sub]
  (update state :name->sub assoc name sub))

(defn state-after-new-qvns
  [state name qvns]
  (update state :name->qvns-set update name (fnil conj #{}) qvns))

(defn sub
  [state name]
  (-> state :name->sub (get name)))
