(ns amps-excel-plugin.amps.functional-test
  (:require [amps-excel-plugin.amps.functional :as sut]
            [clojure.test :as t]))

(t/deftest components-test
  (t/is (= {:host-port "localhost:8080"
            :message-type "json"}
           (sut/components "tcp://localhost:8080/amps/json"))))
