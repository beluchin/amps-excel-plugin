(ns amps-excel-plugin.excel.functions
  (:gen-class
   :name amps_excel_plugin.excel.Functions
   :prefix "java-"
   :methods [^:static [^{com.exceljava.jinx.ExcelFunction {}}
                       subscribe [String String] com.exceljava.jinx.Rtd]
             ^:static [^{com.exceljava.jinx.ExcelFunction {:autoResize true}}
                       expand [java.lang.Object] "[[Ljava.lang.Object;"]


             ^:static [^{com.exceljava.jinx.ExcelFunction {}}
                       oneInt [] int]
             ^:static [^{com.exceljava.jinx.ExcelFunction {:autoResize true}}
                       twoInts [] "[[Ljava.lang.Object;"]
             ^:static [^{com.exceljava.jinx.ExcelFunction {}}
                       twoIntVectorRtd [] com.exceljava.jinx.Rtd]
             ^:static [^{com.exceljava.jinx.ExcelFunction {:autoResize true}}
                       expandVec [java.lang.Object] "[[Ljava.lang.Object;"]])
  (:require [amps-excel-plugin.amps :as amps]
            [amps-excel-plugin.core :as core]
            [amps-excel-plugin.shell :as shell]
            [amps-excel-plugin.shell.logging :as logging]
            [cheshire.core :as json]
            [clojure.pprint :as pprint])
  (:import [com.crankuptheamps.client Client Command MessageHandler]))

(def ^:constant logger
  (logging/new-logger "amps_excel_plugin.excel.Functions"))

(defn java-subscribe
  [uri topic]
  (let [client  (Client. (amps/new-client-name))
        command (.. (Command. "subscribe") (setTopic topic))
        rtd     (com.exceljava.jinx.Rtd.)
        handler (reify MessageHandler
                  (invoke [_ msg]
                    (let [json (.getData msg)
                          rows (core/rows (json/parse-string json))]
                      (logging/info logger json)
                      (.notify rtd rows)
                      (logging/info logger rows))))]
    (doto client
      (.connect uri)
      (.logon)
      (.executeAsync command handler))
    rtd))

(defn java-expand
  [x]
  (logging/info logger x)
  (if (= clojure.lang.LazySeq (type x))
    (to-array-2d x)
    (to-array-2d [["pending"]])))




(declare two-random-ints-thread two-random-ints)

(defn java-oneInt [] 42)

(defn java-twoInts [] (to-array-2d [[42 43]]))

(defn java-twoIntVectorRtd []
  (let [rtd (com.exceljava.jinx.Rtd.)
        thread (two-random-ints-thread rtd)]
    (.notify rtd [[42 42]])
    (.start thread)
    rtd))

(defn- two-random-ints-thread
  [rtd]
  (shell/new-rtd-thread
    (shell/until-interrupted-subr
      (shell/execute-at-rate-or-ignore-subr
        (fn [] (let [random-ints [[(rand-int 30) (rand-int 42)]]]
                 (logging/info logger (with-out-str (pprint/pprint random-ints)))
                 (.notify rtd random-ints)))
        0.2))))

(defn java-expandVec
  [v]
  (if (= clojure.lang.PersistentVector (type v))
    (to-array-2d v)
    (to-array-2d [["pending"]])))

(comment
  (println [1 2])
  )
