(ns simple-amps.functional.state)

(comment 
  ;; this is the shape of the state
  {;; user defined
   :alias->sub ...
   :alias->qvns-set ...

   ;; implementation
   :uri->executor ...
   :alias->ampsies ...})

(defn executor
  [state uri]
  (get-in state :uri->executor uri))

(defmulti qvns-set #(if (map? %2) :subscription :alias))
(defmethod qvns-set :subscription
  [state sub]
  (let [sub->alias (clojure.set/map-invert (:alias->sub state))]
    (-> sub->alias
        (get sub)
        ((:alias->qvns-set state)))))
(defmethod qvns-set :alias
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
