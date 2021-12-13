(ns simple-amps
  (:refer-clojure :exclude [alias filter require])
  (:require [clojure.string :as str]
            [simple-amps.consumer :as c]
            [simple-amps.impl :as f]
            [simple-amps.operational :as o]))

(defn query-value-and-subscribe
  "returns nil or an error when the args are malformed"
  [alias filter nested-map-expr value-expr consumer qvns-call-id]

  (let [qvns-or-error (f/qvns-or-error (str/trim filter)
                                       (str/trim nested-map-expr)
                                       (str/trim value-expr)
                                       consumer)]
    (if (f/error? qvns-or-error)
      qvns-or-error
      (let [uri (o/save (str/trim alias) qvns-or-error qvns-call-id)]
        (if uri
          (o/async-revisit uri)
          (c/on-inactive consumer "undefined alias"))))))

(defn require
  "If the uri is malformed, it will be notified to 
  consumers provided on the related query-value-and-subscribe calls."
  ([alias uri topic] (require alias uri topic nil))

  ([alias uri topic optfilter]
   (let [trimmed-uri (str/trim uri)]
     (o/save (str/trim alias) (f/subscription trimmed-uri
                                              (str/trim topic)
                                              (and optfilter (str/trim optfilter))))
     (o/async-revisit trimmed-uri))))

(defn unsubscribe
  "consider returning an error when the id does not match a qvns"
  [qvns-call-id]
  (when-let [uri (o/remove-qvns-call-id qvns-call-id)]
    (o/async-revisit uri)))
