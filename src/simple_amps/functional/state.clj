(ns simple-amps.functional.state)

(defn executor
  [state uri]
  (get-in state :uri->executor uri))

(defn qvns-coll
  [state n]
  (get-in state :name->qvns-set n))

(defn state-after-new-ampsies
  [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(defn state-after-new-alias
  [state n sub]
  (assoc-in state [:name->sub n] sub))

(defn state-after-new-executor-if-absent
  [state uri executor]
  (update-in state [:uri->executor uri] #(or % executor)))

(defn state-after-new-qvns
  [state n qvns]
  (update-in state [:name->qvns-set n] (fnil conj #{}) qvns))

(defn sub
  [state n]
  (get-in state :name->sub n))
