(ns simple-amps
  (:refer-clojure :exclude [alias filter require])
  (:require [simple-amps.functional :as f]
            [simple-amps.operational :as o]))

(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [^String alias
   ^String filter
   ^String nested-map-expr
   ^String value-expr
   consumer
   qvns-call-id]
  (let [qvns-or-error (f/qvns-or-error filter nested-map-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (do (o/put-qvns alias qvns-or-error qvns-call-id)
          (o/on-query-value-and-subscribe alias qvns-or-error)
          nil))))

(declare on-aliased save-alias)
(defn require
  "Returns any other aliases associated with the same subscription or nil.

  It may result in trying to connect to AMPS - as long as a related 
  query-value-and-subscribe is active.

  If the uri is malformed, this will be notified via the 
  query-value-and-subscribe calls."
  ([^String s ^String uri ^String topic] (require s uri topic nil))

  ([^String s ^String uri ^String topic ^String filter]
   (let [sub (f/subscription uri topic filter)]
      (o/put-alias s sub)
      (o/on-require s sub)
      nil)))

(defn unsubscribe
  "returns [alias qvns] that was unsubscribed or nil if there was none"
  [x]
  (let [alias+qvns (o/remove-qvns-call-id x)]
    (when alias+qvns (o/on-unsubscribed alias+qvns))
    alias+qvns))
