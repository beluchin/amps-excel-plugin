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
  ([state] (sut/ensure state (qvns)))
  ([state & overrides] (sut/ensure state (apply qvns overrides))))

(declare state subscribed)
(defn- ensured-subscription-state []
  (-> nil ensure state subscribed state))

(defn- handle-message [state]
  (throw (UnsupportedOperationException.)))

(def ^:private state sut/state)

(defn- qvns
  ([] {:callbacks       :callbacks
       :value-extractor :value-extractor
       :filter-expr     :qvns-filter-expr
       :msg-stream      {:uri         :uri
                         :topic       :topic
                         :filter-expr :msg-stream-filter-expr}})
  ([& {:as overrides}]
   (let [override-key->keys {:topic            [:msg-stream :topic]
                             :qvns-filter-expr [:filter-expr]}]
     (reduce (fn [m [k v]] (assoc-in m (get override-key->keys k [k]) v))
             (qvns)
             overrides))))

(defn- remove
  ([state] (remove state :qvns))
  ([state qvns] (sut/remove state qvns)))

(defn- replace-filter []
  (sut/->ReplaceFilter :content-filter :sub-id :command-id))

(declare subscribe-args)
(defn- subscribe
  ([] (sut/->Subscribe (subscribe-args)))
  ([& overrides]
   (sut/->Subscribe (apply subscribe-args overrides))))

(defn- subscribe-args
  ([]
   (let [qvns (qvns)]
     [{:topic (qvns/topic qvns)
       :content-filter (andor/and (qvns/filter-expr qvns)
                                  (qvns/msg-stream-filter-expr qvns))
       :callbacks (qvns/callbacks qvns)}]))
  ([& {:as overrides}]
   [(merge (helpers/single (subscribe-args)) overrides)]))

(defn- subscribed 
  ([state] (sut/subscribed state :uri :topic :content-filter :sub-id :command-id)))

(defn- unsubscribe [state]
  (throw (UnsupportedOperationException.)))

(t/deftest ensure-test
  (t/testing "basic subscribe"
    (t/is (= (subscribe) (-> nil ensure decision)))

    (t/testing "subscribe to other qvns"
      (t/testing
          "one subscription i.e. diff qvns filters, same msg-stream filter"
          (t/is (= (subscribe :content-filter
                              (andor/and :msg-stream-filter-expr
                                         (andor/or :qvns-filter-expr
                                                   :qvns-filter-expr-2)))
                   (-> (ensured-subscription-state)
                       (ensure :qvns-filter-expr :qvns-filter-expr-2)
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
