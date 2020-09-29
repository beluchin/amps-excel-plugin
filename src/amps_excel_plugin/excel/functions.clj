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
            [amps-excel-plugin.excel :as excel]
            [amps-excel-plugin.logging :as logging]
            [amps-excel-plugin.state :as state]
            [cheshire.core :as json])
  (:import com.exceljava.jinx.Rtd))

(declare get-vector-2d)
(defn java-expand
  [s?]
  (to-array-2d (get-vector-2d s?)))

(declare new-rtd new-subscription-id new-subscription)
(defn java-subscribe
  [uri topic]
  (logging/info (str "subscribe uri:" uri " topic:" topic))
  (let [subscription-id (new-subscription-id uri topic)
        rtd             (new-rtd subscription-id)
        subscription    (new-subscription uri topic rtd subscription-id)]

    (state/assoc-subscription subscription-id subscription)

    ;; notifying the rtd with the subscription id 
    ;; makes Excel show the latter on the cell where the 
    ;; this function was called.
    (.notify rtd subscription-id)

    rtd))

(declare unsubscribe)
(defn java-unsubscribe
  [s]
  (logging/info (str "unsubscribe " s))
  (if (.contains s "**unsubscribed**")

    ;; excel calls this function again with the string displayed
    ;; on the subscription cell after unsubscribing
    "OK"
    
    (let [subscription? (state/find-subscription s)]
      (if subscription?
        (do (unsubscribe s subscription?)
            "OK")
        "invalid subscription"))))

(defn- get-vector-2d
  [s?]
  (if (state/subscription? s?)
    (let [json? (state/find-data s?)]
      (if json?
        (core/rows (json/parse-string json?))
        [["pending"]]))
    [["invalid subscription"]]))

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
  [uri topic rtd subscription-id]
  (let [json-consumer (fn [json]
                        (state/assoc-data-if-subscribed subscription-id json)
                        (.notify rtd subscription-id))]
    (-> json-consumer
        (amps/new-json-subscription uri topic)
        (assoc ::excel/rtd rtd))))

(defn- new-subscription-id
  [uri topic]
  (state/new-subscription-id uri topic))

(defn- rtd [subscription] (::excel/rtd subscription))

(defn- unsubscribe
  [id subscription]
  (amps/unsubscribe subscription)
  (state/dissoc id)
  (.notify (rtd subscription) (str "**unsubscribed** " id)))
