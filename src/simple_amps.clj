(ns simple-amps
  (:refer-clojure :exclude [alias filter])
  (:require [simple-amps.functional :as f]
            [simple-amps.operational :as o]))

(declare on-aliased save-alias)
(defn alias
  "Returns any other aliases associated with the same subscription or nil.

  The connection to AMPS will take place only after a query-value-and-subscribe 
  references the alias.

  No validation of uri occurs here. If the uri is malformed it will
  be notified via the query-value-and-subscribe calls."
  ([^String s ^String uri ^String topic] (alias s uri topic nil))

  ([^String s ^String uri ^String topic ^String filter]
   (let [sub (f/subscription uri topic filter)]
      (o/save-alias s sub)
      (o/on-aliased s sub)
      nil)))

(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [^String alias ^String filter ^String context-expr ^String value-expr consumer]
  (let [qvns-or-error (f/qvns-or-error filter context-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (do (o/save-qvns alias qvns-or-error)
          (o/on-query-value-and-subscribe alias)
          nil))))
