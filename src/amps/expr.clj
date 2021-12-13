(ns amps.expr)

(defprotocol StringForm
  (string-form [cf] "the string that can be used on a subscription as the content filter"))
(extend-protocol StringForm
  String
  (string-form [cf] cf))

