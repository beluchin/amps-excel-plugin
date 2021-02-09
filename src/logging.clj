(ns logging
  (:require [clojure.pprint :as pprint]))

(declare default-logger)

(defn info
  ([x] (info default-logger x))
  ([logger x] (.info logger (with-out-str (pprint/pprint x)))))

(defn new-logger
  "All logging from Java is done using the 
  standard logging package java.util.logging.
  See also: https://exceljava.com/docs/config/logging.html"
  [s]
  (java.util.logging.Logger/getLogger s))

(def ^:constant default-logger
  (new-logger "amps_excel_plugin"))


