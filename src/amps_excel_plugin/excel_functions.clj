(ns amps-excel-plugin.excel-functions
  (:refer-clojure :exclude [alias filter require])
  (:gen-class
   :name amps_excel_plugin.excel.Functions
   :prefix "java-"
   :methods [^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.require"}}
              require
              [String String String String]
              String]

             ;; --
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.queryValueAndSubscribe"}}
              queryValueAndSubscribe
              [String String String String]
              com.exceljava.jinx.Rtd]])
  (:require logging simple-amps
            [simple-amps.consumer :as consumer])
  (:import com.exceljava.jinx.Rtd))

(declare new-rtd)
(defn java-queryValueAndSubscribe
  [^String alias ^String message-filter ^String context-expr ^String value-expr]
  (logging/info (str "qvns alias:" alias " message-filter:" message-filter
                     " context-expr:" context-expr " value-expr:" value-expr))
  (let [rtd (new-rtd)
        consumer (reify consumer/QueryValueAndSubscribeConsumer
                   (on-value [_ x] (.notify rtd x))
                   (on-oof [_ x] (.notify rtd (str "oof: " x)))

                   (on-activating [_] (.notify rtd "activating"))
                   (on-activated [_] (.notify rtd "activated"))
                                          
                   (on-inactive [_ reason] (.notify rtd (str "inactive: " reason))))
        error (simple-amps/query-value-and-subscribe alias
                                                     message-filter
                                                     context-expr
                                                     value-expr
                                                     consumer)]
    (when error (.notify rtd error))
    rtd))

(defn java-require
  "all args are required except the filter"
  [^String alias ^String uri ^String topic ^String filter]
  (logging/info (str "require alias:" alias " uri:" uri
                     " topic:" topic " filter:" filter))
  (simple-amps/require alias uri topic filter)
  "OK")

(defn- new-rtd
  []
  (proxy [Rtd] []
    (onDisconnected [] (logging/info "rtd disconnected"))))
