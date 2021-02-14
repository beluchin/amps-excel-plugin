(ns simple-amps.functional.state)

(comment 
  ;; this is the shape of the state
  {;; user defined
   :alias->sub ...
   :alias->qvns-set ...

   ;; implementation
   :uri->executor ...
   :alias->ampsies ...
   :uri->client ...})

(defn client
  [state uri]
  (get-in state [:uri->client uri]))

(defn executor
  [state uri]
  (get-in state [:uri->executor uri]))

(defmulti qvns-set #(when (map? %2) :subscription))
(defmethod qvns-set :subscription
  [state sub]
  (let [sub->alias (clojure.set/map-invert (:alias->sub state))]
    (-> sub->alias
        (get sub)
        ((:alias->qvns-set state)))))
(defmethod qvns-set :default
  [state a]
  (get-in state [:alias->qvns-set a]))

(defn state-after-new-ampsies
  [state sub ampsies]
  (assoc-in state [:sub->ampsies sub] ampsies))

(defn state-after-new-alias
  [state a sub]
  (assoc-in state [:alias->sub a] sub))

(defn state-after-new-client-if-absent
  [state uri client]
  (update-in state [:uri->client uri] #(or % client)))

(defn state-after-new-executor-if-absent
  [state uri executor]
  (update-in state [:uri->executor uri] #(or % executor)))

(defn state-after-new-qvns
  [state a qvns]
  (update-in state [:alias->qvns-set a] (fnil conj #{}) qvns))

(defn sub
  [state a]
  (get-in state [:alias->sub a]))
