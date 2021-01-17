(ns amps-excel-plugin.excel.functions
  (:gen-class
   :name amps_excel_plugin.excel.Functions
   :prefix "java-"
   :methods [
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.subscribe"}}
              subscribe [String String] String #_com.exceljava.jinx.Rtd]

             ;; --
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.getValue"}}
              getValue [java.lang.Object String String String] com.exceljava.jinx.Rtd]

             ;; --
             ;;^:static
             #_ [^{com.exceljava.jinx.ExcelFunction {:value "amps.expand"
                                                  :autoResize true}}
              expand [java.lang.Object] "[[Ljava.lang.Object;"]

             ;; --
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.unsubscribe"}}
              unsubscribe [String] String]
             ])
  (:require [amps-excel-plugin.amps :as amps]
            [amps-excel-plugin.amps.functional :as amps.functional]
            [amps-excel-plugin.core :as core]
            [amps-excel-plugin.excel :as excel]
            [amps-excel-plugin.functional :as functional]
            [amps-excel-plugin.logging :as logging]
            [amps-excel-plugin.state :as state]
            [cheshire.core :as json])
  (:import com.exceljava.jinx.Rtd))


#_(declare vector-2d)
#_(defn java-expand
  [rtd]
  ;; no logging because it is high frequency
  (to-array-2d
    (let [subscription (state/try-get rtd)]
      (cond
        (nil? subscription) [["invalid subscription"]]
        (nil? (:data subscription)) [["pending"]]
        :else (vector-2d (:data subscription))))))

(declare new-rtd)
(defn java-getValue
  "returns an rtd. The subscription does not need to be active"
  [subscription-alias message-filter-expr context-expr value-expr]
  (let [rtd (new-rtd)]
    (.notify rtd 42)
    rtd))

(declare subscribe-and-get-alias)
(defn java-subscribe
  "returns a string that contains a subscription alias"
  [uri topic]
  (logging/info (str "subscribe uri:" uri " topic:" topic))
  (str "OK: " (subscribe-and-get-alias uri topic)))

(declare unsubscribe)
(defn java-unsubscribe
  [s]
  (logging/info (str "unsubscribe " s))
  (if (.contains s "**unsubscribed**")

    ;; excel calls this function again with the string displayed
    ;; on the subscription cell after unsubscribing
    "OK"
    
    (let [subscription? (state/try-get-subscription s)]
      (if subscription?
        (do (unsubscribe s subscription?)
            "OK")
        "invalid subscription"))))

(defn- new-rtd
  []
  (proxy [Rtd] []
    ;; supports deleting the cell where the subscription
    ;; was created
    (onDisconnected
      []
      (logging/info "rtd disconnected")))
  #_[subscription-id]
  #_(proxy [Rtd] []
    ;; supports deleting the cell where the subscription
    ;; was created
    (onDisconnected
      []
      (logging/info (str "deleted " subscription-id))
      (amps/unsubscribe (state/get-subscription subscription-id)))))

(defn- subscribe-and-get-alias
  [uri topic]
  (amps/subscribe-and-get uri topic (constantly :no-op))
  (functional/subscription-alias (functional/subscription uri topic)))

(defn- rtd [subscription] (:rtd subscription))

(defn- unsubscribe
  [id subscription]
  (amps/unsubscribe subscription)
  (state/dissoc id)
  (.notify (rtd subscription) (str "**unsubscribed** " id)))

#_(defn- vector-2d
  [json]
  (core/rows (json/parse-string json)))
