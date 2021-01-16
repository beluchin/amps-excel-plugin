(ns amps-excel-plugin.excel.functions
  (:gen-class
   :name amps_excel_plugin.excel.Functions
   :prefix "java-"
   :methods [
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.subscribe"}}
              subscribe [String String] com.exceljava.jinx.Rtd]

             ;; --
             ^:static
             [^{com.exceljava.jinx.ExcelFunction {:value "amps.getValue"}}
              getValue [java.lang.Object String String String] java.lang.Object]

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
            [amps-excel-plugin.core :as core]
            [amps-excel-plugin.excel :as excel]
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

(defn java-getValue
  [rtd-subscription message-filter-expr context-expr value-expr]
  42.42)

(declare new-rtd new-subscription-id new-subscription)
(defn java-subscribe
  [uri topic]
  (logging/info (str "subscribe uri:" uri " topic:" topic))
  (let [id           (new-subscription-id uri topic)
        rtd          (new-rtd id)
        subscription (new-subscription uri topic rtd id)]

    (state/assoc-subscription id subscription)

    ;; notifying the rtd with the subscription id 
    ;; makes Excel show the latter on the cell where the 
    ;; this function was called.
    (.notify rtd id)

    rtd))

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
  [subscription-id]
  (proxy [Rtd] []
    ;; supports deleting the cell where the subscription
    ;; was created
    (onDisconnected
      []
      (logging/info (str "deleted " subscription-id))
      (amps/unsubscribe (state/get-subscription subscription-id)))))

(defn- new-subscription
  [uri topic rtd id]
  (let [json-consumer (fn [json]
                        (state/assoc-data-if-subscribed id json)
                        (.notify rtd id))]
    (-> json-consumer
        (amps/new-json-subscription uri topic)
        (assoc :rtd rtd))))

(defn- new-subscription-id
  [uri topic]
  (state/new-subscription-id uri topic))

(defn- rtd [subscription] (:rtd subscription))

(defn- unsubscribe
  [id subscription]
  (amps/unsubscribe subscription)
  (state/dissoc id)
  (.notify (rtd subscription) (str "**unsubscribed** " id)))

#_(defn- vector-2d
  [json]
  (core/rows (json/parse-string json)))
