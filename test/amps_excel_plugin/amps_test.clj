(ns amps-excel-plugin.amps-test
  (:require [amps-excel-plugin.amps :as amps]
            [clojure.test :as t]))

(declare returning-fn)

(t/deftest get-client
  (t/testing "reuses connections"
    (with-redefs [amps/get-new-client (returning-fn :blah :bleh :blih)]
      (t/is (= :blah (do
                       (amps/get-client "anything")
                       (amps/get-client "anything")))))))

(defn returning-fn 
  "returns a function that returns the args one at time e.g.:
  (def f (returning-fn 1 2 3))
  (f)                      ;; => 1
  (f :can-take-any-arg)    ;; => 2
  (f :multiple :args :too) ;; => 3

  taken from https://stackoverflow.com/a/48374397/614800"
  [x y & more]
  (let [a (atom (concat [x y] more))]
    (fn [& args]
      (let [r (first @a)]
        (swap! a rest)
        r))))
