(ns simple-amps
  (:refer-clojure :exclude [alias filter require])
  (:require [simple-amps.consumer :as c]
            [simple-amps.functional :as f]
            [simple-amps.operational :as o]))

(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [alias filter nested-map-expr value-expr consumer qvns-call-id]
  (let [qvns-or-error (f/qvns-or-error filter nested-map-expr value-expr consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (let [uri (o/save alias qvns-or-error qvns-call-id)]
        (if uri
          (o/async-revisit uri)
          (c/on-inactive consumer "undefined alias"))))))

(defn require
  "If the uri is malformed, it will be notified to 
  consumers provided on the related query-value-and-subscribe calls."
  ([alias uri topic] (require alias uri topic nil))

  ([alias uri topic filter]
   (o/save alias (f/subscription uri topic filter))
   (o/async-revisit uri)))

(defn unsubscribe
  [x]
  (when-let [uri (o/remove-qvns-call-id x)]
    (o/async-revisit uri))
  ;; toremove
  #_(let [alias+qvns (o/remove-qvns-call-id x)]
      (when alias+qvns (o/on-unsubscribed alias+qvns))
      alias+qvns))
