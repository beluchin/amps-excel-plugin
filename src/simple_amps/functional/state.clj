(ns simple-amps.functional.state)

(defn executor
  [state uri]
  (get-in state :uri->executor uri))

(defmulti qvns-set #(map? %2))
(defmethod qvns-set true
  [state sub]
  (throw (UnsupportedOperationException.)))
(defmethod qvns-set false
  [state a]
  (get-in state [:alias->qvns-set a]))

(defn state-after-new-ampsies
  [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(defn state-after-new-alias
  [state a sub]
  (assoc-in state [:alias->sub a] sub))

(defn state-after-new-executor-if-absent
  [state uri executor]
  (update-in state [:uri->executor uri] #(or % executor)))

(defn state-after-new-qvns
  [state a qvns]
  (update-in state [:alias->qvns-set a] (fnil conj #{}) qvns))

(defn sub
  [state a]
  (get-in state :alias->sub a))
