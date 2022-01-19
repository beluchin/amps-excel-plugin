(ns amps.query-value-and-subscribe.callbacks)

(defprotocol Callbacks
  (on-value [this x])
  (on-oof [this x])

  (on-activating [this])
  (on-activated [this])

  ;; reason: cannot connect | connection was closed | undefined alias
  (on-inactive [this reason]))
