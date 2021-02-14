(ns logging
  (:require [clojure.pprint :as pprint]
            [clojure.string :as string]))

(declare default-logger)
(defn error
  ([x] (error default-logger x))
  ([logger x] (.severe logger (cond (string? x) x
                                    :else (with-out-str (pprint/pprint x))))))

(declare default-logger)
(defn info
  ([x] (info default-logger x))
  ([logger x] (.info logger (cond (string? x) x
                                  :else (with-out-str (pprint/pprint x))))))

(defn new-logger
  "All logging from Java is done using the 
  standard logging package java.util.logging.
  See also: https://exceljava.com/docs/config/logging.html"
  [s]
  (java.util.logging.Logger/getLogger s))

(def ^:constant default-logger (new-logger "amps_excel_plugin"))



