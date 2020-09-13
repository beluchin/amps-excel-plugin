(ns amps-excel-plugin.excel.functions
  (:gen-class
   :name amps_excel_plugin.excel.Functions
   :prefix "java-"
   :methods [
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.getNewSubscription"}}
              getNewSubscription [String String] com.exceljava.jinx.Rtd]

             ;; --
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.expand"
                                                  :autoResize true}}
              expand [java.lang.Object] "[[Ljava.lang.Object;"]

             ;; --
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.unsubscribe"
                                                  :isMacroType true}}
              unsubscribe [String] String]
             ])
  (:require [amps-excel-plugin.amps :as amps]
            [amps-excel-plugin.core :as core]
            [amps-excel-plugin.state :as state]
            [cheshire.core :as json]
            [amps-excel-plugin.excel :as excel])
  (:import [com.exceljava.jinx ExcelAddIn Rtd ExcelReference]))

(defn java-expand
  [s]
  (to-array-2d
    (if (not (.contains s "unsubscribed"))
      (let [subscription-id s
            json            (state/get-data subscription-id)]
        (if json
          (core/rows (json/parse-string json))
          [["pending"]]))
      [["**unsubscribed**"]])))

(defn java-getNewSubscription
  [uri topic]
  (let [subscription-id (state/get-new-subscription-id uri topic)
        rtd             (Rtd.)
        json-consumer   (fn [json]
                          (println "here!")
                          (state/put-data subscription-id json)
                          (.notify rtd subscription-id))
        subscription    (amps/get-new-json-subscription uri topic json-consumer)]
    
    (state/put-subscription subscription-id
                            (assoc subscription ::excel/rtd rtd))

    ;; notifying the rtd with the subscription id (string) 
    ;; makes Excel show the string on the cell where the 
    ;; subscribe function was called.
    (.notify rtd subscription-id)

    rtd))

(defn java-unsubscribe
  [s]
  (when (not (.contains s "unsubscribed"))
    (let [subscription-id s
          subscription    (state/get-subscription subscription-id)]
      (amps/unsubscribe subscription)
      (.notify (::excel/rtd subscription)
               (str "**unsubscribed** " subscription-id))))
  "OK")

(comment
  (println [1 2])
  )
