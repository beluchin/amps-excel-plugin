(ns amps.query-value-and-subscribe.qvns
  (:require [amps.query-value-and-subscribe.callbacks :as callbacks]
            andor))

(defn callbacks [qvns]
  (:callbacks qvns))

(defn msg-stream [qvns]
  (:msg-stream qvns))

(defn msg-stream-filter-expr [qvns]
  (get-in qvns [:msg-stream :filter-expr]))

(defn qvns-filter-expr [qvns]
  (:filter-expr qvns))

(defn topic [qvns]
  (get-in qvns [:msg-stream :topic]))
