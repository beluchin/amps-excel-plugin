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

(declare get-2dim-vector)
(defn java-expand
  [s]
  (to-array-2d
    (if (not (.contains s "unsubscribed"))
      (get-2dim-vector s)
      [["**unsubscribed**"]])))

(declare new-rtd new-subscription-id new-subscription)
(defn java-subscribe
  [uri topic]
  (let [subscription-id (new-subscription-id uri topic)
        rtd             (new-rtd subscription-id)
        subscription    (new-subscription uri topic rtd subscription-id)]

    (state/put-subscription subscription-id subscription)

    ;; notifying the rtd with the subscription id 
    ;; makes Excel show the latter on the cell where the 
    ;; this function was called.
    (.notify rtd subscription-id)

    rtd))

(declare rtd)
(defn java-unsubscribe
  [s]
  (when (not (.contains s "unsubscribed"))
    (let [subscription (state/get-subscription s)]
      (amps/unsubscribe subscription)
      (.notify (rtd subscription) (str "**unsubscribed** " s))))
  "OK")

(defn- get-2dim-vector
  [subscription-id]
  (let [json (state/get-data subscription-id)]
    (if json
      (core/rows (json/parse-string json))
      [["pending"]])))

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
                        (state/put-data subscription-id json)
                        (.notify rtd subscription-id))]
    (-> json-consumer
        (amps/new-json-subscription uri topic)
        (assoc ::excel/rtd rtd))))

(defn- new-subscription-id
  [uri topic]
  (state/new-subscription-id uri topic))

(defn- rtd [subscription] (::excel/rtd subscription))

(comment
  (println [1 2])
  )
