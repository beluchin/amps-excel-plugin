(ns simple-amps.consumer)

(defprotocol QueryValueAndSubscribeConsumer
  (on-value [this x])
  (on-activating [this])
  (on-activated [this])
  (on-inactive [this reason]) ;; reason: cannot connect | connection was closed
  (on-unaliased [this]))
