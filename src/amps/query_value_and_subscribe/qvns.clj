(ns amps.query-value-and-subscribe.qvns
  (:require [amps.query-value-and-subscribe.callbacks :as callbacks]
            andor))

(defn activating-runnable [qvns]
  (partial callbacks/on-activating (:callbacks qvns)))

(defn content-filter [qvns]
  (andor/and (get-in qvns [:msg-stream :filter-expr])
             (get-in qvns [:msg-stream :mq-msg-stream :filter-expr])))

(defn topic [qvns]
  (get-in qvns [:msg-stream :mq-msg-stream :topic]))
