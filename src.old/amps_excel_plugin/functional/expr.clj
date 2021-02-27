(ns amps-excel-plugin.functional.expr
  (:require [clojure.string :as string]
            [functional :as functional]))

(defprotocol Expr
  (common-path [this])
  (evaluate [this m]))

(defrecord ConstantExpr [x]
  Expr
  (common-path [_] nil)
  (evaluate [_ m] x))

;; ks is a non-empty sequence of keys
(defrecord BinaryExpr [lhs-expr op rhs-expr]
  Expr
  (common-path [_] (functional/common-prefix (common-path lhs-expr)
                                             (common-path rhs-expr)))
  (evaluate [_ m] (op (evaluate lhs-expr m) (evaluate rhs-expr m))))

(defrecord IsNullExpr [x])

(defrecord NotExpr [x])

(defrecord ValueExpr [ks]
  Expr
  (common-path [_] ks)
  (evaluate [_ m] (get-in m ks)))

(defn parse-constant-expr
  [s]
  (->ConstantExpr (-> s
                      (.replaceAll "'" "\"")
                      clojure.edn/read-string)))

(declare parse-value-expr)
(defn parse-binary-expr
  [s]
  (let [tokens (string/split s #" ")]
    (->BinaryExpr (parse-value-expr (first tokens))
                  (var-get (resolve (symbol (second tokens))))
                  (parse-constant-expr (last tokens)))))

(defn parse-value-expr
  [s]
  (when (re-matches #"(/[a-zA-Z][a-zA-Z0-9]*)+" s)
    (->ValueExpr (rest (string/split s #"/")))))