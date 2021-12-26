(ns helpers)

(defn assoc-if
  "Same as assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> kvs
       (partition 2)
       (filter second)
       (map vec)
       (into m)))

(defn assoc-if-missing [m k v]
  (if (m k) m (assoc m k v)))

(defn assoc-in-if-missing [m ks v]
  (if (get-in m ks) m (assoc-in m ks v)))

(defn common-prefix
  "(... [:a :b] [:a :c]) => [:a]"
  [coll1 coll2]
  {:pre [(nil? coll2)]}
  coll1)

(defn get-pid []

  ;; String vmName = ManagementFactory.getRuntimeMXBean().getName();
  ;; int p = vmName.indexOf("@");
  ;; String pid = vmName.substring(0, p);
  ;; System.out.println(pid);

  ;; see also: https://shekhargulati.com/2015/11/16/how-to-programmatically-get-process-id-of-a-java-process/

  (let [name (.. (java.lang.management.ManagementFactory/getRuntimeMXBean)
                 (getName))]
    (.substring name 0 (.indexOf name "@"))))

(defn index-of-first
  "https://stackoverflow.com/a/51145673/614800"
  [pred coll]
  (ffirst (filter (comp pred second) (map-indexed list coll))))

(defn leafpaths
  ;; https://stackoverflow.com/a/21769786/614800
  "a leafpath is a sequence of keys from the top to a value that is not a map"
  [m]
  (if (map? m)
    (vec 
     (mapcat (fn [[k v]]
               (let [sub (leafpaths v)
                     nested (map #(into [k] %) (filter (comp not empty?) sub))]
                 (if (seq nested)
                   nested
                   [[k]])))
             m))
    []))

(defn single 
  "https://stackoverflow.com/a/14851515/614800"
  [seq]
  (if (clojure.core/seq (rest seq))
   (throw (RuntimeException. "should have precisely one item, but had at least 2"))
   (if (clojure.core/seq seq)
     (first seq)
     (throw (RuntimeException. "should have precisely one item, but had 0")))))
