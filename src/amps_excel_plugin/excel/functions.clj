(ns amps-excel-plugin.excel.functions
  (:gen-class
   :name amps_excel_plugin.excel.Functions
   :prefix "java-"
   :methods [^:static [^{com.exceljava.jinx.ExcelFunction {}}
                       subscribe [String String] com.exceljava.jinx.Rtd]
             ^:static [^{com.exceljava.jinx.ExcelFunction {:autoResize true}}
                       expand [java.lang.Object] "[[Ljava.lang.Object;"]])
  (:require [amps-excel-plugin.amps :as amps]
            [amps-excel-plugin.core :as core]
            [amps-excel-plugin.excel :as excel]
            [cheshire.core :as json]))

(defrecord RtdPayload [toStringValue]
  Object
  (toString [_] toStringValue))

(defn java-subscribe
  [uri topic]
  (let [subscription  (format "%s@%s" topic uri)
        rtd           (com.exceljava.jinx.Rtd.)
        json-consumer (fn [json]
                        (swap! excel/subscription->data assoc subscription json)
                        (.notify rtd subscription))]
    (amps/subscribe-json uri topic json-consumer)

    ;; notifying the rtd with the subscription (string) 
    ;; makes Excel show the string on the cell where the 
    ;; subscribe function was called.
    (.notify rtd subscription)
    
    rtd))

(defn java-expand
  [subscription]
  (let [json (@excel/subscription->data subscription)]
    (if json
      (to-array-2d (core/rows (json/parse-string json)))
      (to-array-2d [["pending"]]))))


(comment
  (println [1 2])
  )
