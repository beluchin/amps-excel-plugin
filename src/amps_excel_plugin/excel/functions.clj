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
            [amps-excel-plugin.shell :as shell]
            [amps-excel-plugin.shell.logging :as logging]
            [cheshire.core :as json]
            [clojure.pprint :as pprint])
  (:import [com.crankuptheamps.client Client Command MessageHandler]))


(defn java-subscribe
  [uri topic]
  (let [client  (Client. (amps/new-client-name))
        command (.. (Command. "subscribe") (setTopic topic))
        rtd     (com.exceljava.jinx.Rtd.)
        handler (reify MessageHandler
                  (invoke [_ msg]
                    (let [json (.getData msg)
                          rows (core/rows (json/parse-string json))]
                      (.notify rtd rows))))]
    (doto client
      (.connect uri)
      (.logon)
      (.executeAsync command handler))
    rtd))

(defn java-expand
  [x]
  (if (= clojure.lang.LazySeq (type x))
    (to-array-2d x)
    (to-array-2d [["pending"]])))


(comment
  (println [1 2])
  )
