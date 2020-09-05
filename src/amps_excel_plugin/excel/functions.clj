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
            [cheshire.core :as json])
  (:import [com.crankuptheamps.client Client Command MessageHandler]))

(defrecord RtdPayload [toStringValue]
  Object
  (toString [_] toStringValue))

(defn java-subscribe
  [uri topic]
  (let [subscription (format "topic:%s uri:%s" topic uri)
        client       (Client. (amps/new-client-name))
        command      (.. (Command. "subscribe") (setTopic topic))
        rtd          (com.exceljava.jinx.Rtd.)
        handler      (reify MessageHandler
                       (invoke [_ msg]
                         (let [json (.getData msg)
                               rows (core/rows (json/parse-string json))]
                           (swap! excel/subscription->rows assoc subscription rows)
                           (.notify rtd subscription))))]
    (doto client
      (.connect uri)
      (.logon)
      (.executeAsync command handler))
    (.notify rtd subscription)
    rtd))

(defn java-expand
  [subscription]
  (to-array-2d (get @excel/subscription->rows subscription [["pending"]])))


(comment
  (println [1 2])
  )
