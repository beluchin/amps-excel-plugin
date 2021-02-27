(defproject amps-excel-plugin-core "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.10.0"]
                 [com.exceljava/jinx "2.1.0"]
                 [com.google.guava/guava "29.0-jre"]
                 [com.crankuptheamps/client "5.3.0.4"]]

  :repl-options {:init-ns simple-amps.operational}

  ;; flycheck-clojure setup
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:dev {:env {:squiggly {:checkers [:kibit :eastwood]
                                    :eastwood-exclude-linters [:unlimited-use]
                                    :eastwood-options {:add-linters [:unused-fn-args]
                                                       ;; :builtin-config-files ["myconfigfile.clj"]
                                                       }}}}}

  :aot [amps-excel-plugin.excel-functions]
  )
