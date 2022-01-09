(ns test-helpers)

(defmacro map-of-keywords [& syms]
  (zipmap (map keyword syms) (map keyword syms)))

