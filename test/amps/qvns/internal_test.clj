(ns amps.qvns.internal-test
  (:refer-clojure :exclude [ensure remove])
  (:require [amps.qvns.internal :as sut]
            [clojure.test :as t]
            test-helpers))

(def ^:private action sut/action)

(defn- consume-value [x]
  (throw (UnsupportedOperationException.)))

(defn- disconnect []
  (sut/->Disconnect :uri))

(declare qvns)
(defn- ensure
  ([state] (ensure state (qvns)))
  ([state qvns] (sut/ensure state qvns)))

(declare state subscribed)
(defn- ensured-subscription-state []
  (-> nil ensure state subscribed state))

(defn- handle-message [state]
  (throw (UnsupportedOperationException.)))

(defn- initial-subscription []
  (sut/->InitialSubscription [[:topic :content-filter]] [:activating-runnable]))

(def ^:private state sut/state)

(defn- msg-stream [& {:as overrides}]
  (throw (UnsupportedOperationException.)))

(defn- qvns [& {:as overrides}]
  {:value-extractor :value-extractor
   :msg-stream      {:filter-expr    :filter-expr
                     :mq-msg-stream  (test-helpers/map-of-keywords
                                       uri
                                       topic
                                       filter-expr)}})

(defn- remove
  ([state] (remove state :qvns))
  ([state qvns] (sut/remove state qvns)))

(defn- replace-filter []
  (sut/->ReplaceFilter :content-filter :sub-id :command-id))

(defn- subscribed [state]
  (sut/subscribed state :uri :topic :content-filter :sub-id :command-id))

(defn- unsubscribe [state]
  (throw (UnsupportedOperationException.)))

(t/deftest ensure-test
  (t/testing "initial subscription"
    (t/is (= (initial-subscription) (-> nil ensure action)))

    (t/testing "decide to take action related to other qvns"
      (t/testing "same topic"
        (throw (UnsupportedOperationException.)))
    
      (t/testing "different topic"
        (throw (UnsupportedOperationException.)))))

  (t/testing "replace filter"
    (t/is (= (replace-filter)
             (-> nil
                 ensure
                 state
                 subscribed
                 state
                 (ensure (qvns (msg-stream :filter-expr :filter-expr-2)))
                 action))))

  (t/testing "consume value"
    ;; subscription is already in place and a value is available
    (t/is (= (consume-value 42)
             (-> (ensured-subscription-state)
                 handle-message
                 (ensure (qvns :value-extractor (constantly 42)))
                 action))))

  (t/testing "do nothing"
    ;; subscription is already in place and no message has yet come in
    (t/is (nil? (-> (ensured-subscription-state)
                    (ensure (qvns :value-extractor :value-extractor-2))
                    action)))))

(t/deftest remove-test 
  (t/testing "disconnect"
    (t/is (= (disconnect)
             (-> (ensured-subscription-state)
                 remove
                 action))))

  (t/testing "unsubscribe" 
    (t/is (= (unsubscribe)
             (-> (ensured-subscription-state)
                 (ensure (qvns :topic :topic-y))
                 state
                 subscribed
                 state
                 (remove (qvns :topic :topic-y))
                 action))))

  (t/testing "replace filter"
    (throw (UnsupportedOperationException.)))

  (t/testing "do nothing"
    ;; when there are more qvns associated with the same m-stream
    ;; i.e. multiple value extractors on the same messages.
    ))

