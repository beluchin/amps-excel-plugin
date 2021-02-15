(ns simple-amps.consumer)

(defprotocol QueryValueAndSubscribeConsumer
  (on-value [this x])
  (on-active-no-sow [this])

  ;; reason: cannot connect | connection was closed
  (on-inactive [this reason])

  (on-unaliased [this]))
