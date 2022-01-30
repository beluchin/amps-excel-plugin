(ns amps.query-value-and-subscribe.qvns
  (:require [amps.query-value-and-subscribe.callbacks :as callbacks]
            andor))

(defn callbacks [qvns]
  (:callbacks qvns))

(defn filter-expr [qvns]
  (:filter-expr qvns))

(defn msg-stream-filter-expr [qvns]
  (get-in qvns [:mgs-stream :filter-exp]))

(defn topic [qvns]
  (get-in qvns [:msg-stream :topic]))
