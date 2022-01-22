(ns amps.query-value-and-subscribe.internal-test
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.query-value-and-subscribe.internal :as sut]
            [clojure.test :as t]
            [amps.query-value-and-subscribe.qvns :as qvns]))

(def ^:private decision sut/decision)

(defn- consume-value [x]
  (throw (UnsupportedOperationException.)))

(defn- disconnect []
  (sut/->Disconnect :uri))

(declare qvns)
(defn- ensure
  ([state] (ensure state (qvns)))
  ([state qvns] (sut/ensure state qvns))
  ([state selector x & overrrides] (throw (UnsupportedOperationException.))))

(declare state subscribed)
(defn- ensured-subscription-state
  ([] (-> nil ensure state subscribed state))
  ([selector x] (throw (UnsupportedOperationException.))))

(defn- handle-message [state]
  (throw (UnsupportedOperationException.)))

(def ^:private state sut/state)

(defn- qvns
  ([] {:callbacks       :callbacks
       :value-extractor :value-extractor
       :msg-stream      {:filter-expr   :msg-stream-filter-expr
                         :mq-msg-stream {:uri         :uri
                                         :topic       :topic
                                         :filter-expr :mq-msg-stream-filter-expr}}})
  ([& {:as overrides}]
   (let [override-to-keys {:topic [:msg-stream :mq-msg-stream :topic]}]
     (reduce (fn [m [k v]] (assoc-in m (get override-to-keys k [k]) v))
             (qvns)
             overrides))))

(defn- remove
  ([state] (remove state :qvns))
  ([state qvns] (sut/remove state qvns)))

(defn- replace-filter []
  (sut/->ReplaceFilter :content-filter :sub-id :command-id))

(defn- subscribe []
  (let [qvns (qvns)]
    (sut/->Subscribe [{:topic (qvns/topic qvns)
                       :content-filter (qvns/content-filter qvns)
                       :callbacks (qvns/callbacks qvns)}])))

(defn- subscribed 
  ([state] (sut/subscribed state :uri :topic :content-filter :sub-id :command-id)))

(defn- unsubscribe [state]
  (throw (UnsupportedOperationException.)))

(t/deftest ensure-test
  (t/testing "basic subscribe"
    (t/is (= (subscribe) (-> nil ensure decision)))

    (t/testing "subscribe to other qvns"
      (t/testing
          "one subscription - different msg-stream filters, same
          mq-msg-stream"
          (t/is (= (subscribe :content-filter
                              (andor/and :mq-msg-stream-filter-expr
                                         (andor/or :msg-filter-1
                                                   :msg-filter-2)))
                   (-> (ensured-subscription-state :msg-stream-filter-expr
                                                   :msg-filter-1)
                       (ensure :msg-stream-filter-expr
                               :msg-filter-2)
                       decision))))
    
      #_(t/testing "multiple subscriptions"
          (throw (UnsupportedOperationException.)))))

  #_(t/testing "replace filter"
      (t/is (= (replace-filter)
               (-> nil
                   ensure
                   state
                   subscribed
                   state
                   (ensure (qvns :msg-stream-filter-expr :filter-expr-2))
                   decision))))

  #_(t/testing "consume value"
      ;; subscription is already in place and a value is available
      (t/is (= (consume-value 42)
               (-> (ensured-subscription-state)
                   handle-message
                   (ensure (qvns :value-extractor (constantly 42)))
                   decision))))

  #_(t/testing "do nothing"
      ;; subscription is already in place and no message has yet come in
      (t/is (nil? (-> (ensured-subscription-state)
                      (ensure (qvns :value-extractor :value-extractor-2))
                      decision)))))

(t/deftest remove-test 
  (t/testing "disconnect"
    (t/is (= (disconnect)
             (-> (ensured-subscription-state)
                 remove
                 decision))))

  (t/testing "unsubscribe" 
    (t/is (= (unsubscribe)
             (-> (ensured-subscription-state)
                 (ensure (qvns :topic :topic-y))
                 state
                 (subscribed :topic :topic-y)
                 state
                 (remove (qvns :topic :topic-y))
                 decision))))

  (t/testing "replace filter"
    (throw (UnsupportedOperationException.)))

  (t/testing "do nothing"
    ;; when there are more qvns associated with the same m-stream
    ;; i.e. multiple value extractors on the same messages.
    ))

