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
  ([mgr] (ensure mgr (qvns)))
  ([mgr qvns] (sut/ensure mgr qvns)))

(declare mgr subscribed)
(defn- ensured-subscription-mgr []
  (-> nil ensure mgr subscribed mgr))

(defn- handle-message [mgr]
  (throw (UnsupportedOperationException.)))

(defn- initial-subscription []
  (sut/->InitialSubscription [[:topic :content-filter]] [:activating-runnable]))

(def ^:private mgr sut/mgr)

(defn- m-stream [& {:as overrides}]
  (throw (UnsupportedOperationException.)))

(defn- qvns [& {:as overrides}]
  {:value-expr :value-expr
   :m-stream   {:filter-expr :filter-expr
                :mqm-stream  (test-helpers/map-of-keywords uri
                                                           topic
                                                           filter-expr)}}
  #_(merge (test-helpers/map-of-keywords uri
                                         topic
                                         context-filter
                                         item-filter
                                         value-extractor
                                         event-handlers)
           overrides))

(defn- remove
  ([mgr] (remove mgr :qvns))
  ([mgr qvns] (sut/remove mgr qvns)))

(defn- replace-filter []
  (sut/->ReplaceFilter :content-filter :sub-id :command-id))

(defn- subscribed [mgr]
  (sut/subscribed mgr :uri :topic :content-filter :sub-id :command-id))

(defn- unsubscribe [mgr]
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
                 mgr
                 subscribed
                 mgr
                 (ensure (qvns (m-stream :filter-expr :filter-expr-2)))
                 action))))

  (t/testing "consume value"
    ;; subscription is already in place and a value is available
    (t/is (= (consume-value 42)
             (-> (ensured-subscription-mgr)
                 handle-message
                 (ensure (qvns :value-extractor (constantly 42)))
                 action))))

  (t/testing "do nothing"
    ;; subscription is already in place and no message has yet come in
    (t/is (nil? (-> (ensured-subscription-mgr)
                    (ensure (qvns :value-extractor :value-extractor-2))
                    action)))))

(t/deftest remove-test 
  (t/testing "disconnect"
    (t/is (= (disconnect)
             (-> (ensured-subscription-mgr)
                 remove
                 action))))

  (t/testing "unsubscribe" 
    (t/is (= (unsubscribe)
             (-> (ensured-subscription-mgr)
                 (ensure (qvns :topic :topic-y))
                 mgr
                 subscribed
                 mgr
                 (remove (qvns :topic :topic-y))
                 action))))

  (t/testing "replace filter"
    (throw (UnsupportedOperationException.)))

  (t/testing "do nothing"
    ;; when there are more qvns associated with the same m-stream
    ;; i.e. multiple value extractors on the same messages.
    ))

