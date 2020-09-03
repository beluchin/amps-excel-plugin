(ns amps-excel-plugin.shell
  (:import com.google.common.util.concurrent.RateLimiter))

(defn execute-at-rate-or-ignore-subr
  "returns a new sub-routine that will forward the to the given sub-routine 
  as long as the call frequency does not exceed the given rate (per second)
  ignoring calls otherwise

  the rate could be a fraction which indicates the subroutine will be 
  executed at rate of less than one per second"
  [subr rate]
  (let [rate-limiter (RateLimiter/create rate)]
    (fn [& args] (when (.tryAcquire rate-limiter)
                   (apply subr args)))))

(defn new-rtd-thread
  [runnable]
  (let [thread (Thread. runnable)]
    (.setDaemon thread true)
    thread))

(defn until-interrupted-subr
    "see also: https://codeahoy.com/java/How-To-Stop-Threads-Safely/"
    [subr]
    (fn [] (loop []
             (when (not (.isInterrupted (Thread/currentThread)))
               (subr)
               (Thread/sleep 5)
               (recur)))))

(comment
  (def t (Thread. (until-interrupted-subr (fn [] ))))
  (.isAlive t)
  (.start t)
  (.isAlive t)
  (.interrupt t)

  (def t (Thread.
           (until-interrupted-subr
             (execute-at-rate-or-ignore-subr
               (fn [] (println "here"))
               0.1)))) ; once every 10 seconds
  )
