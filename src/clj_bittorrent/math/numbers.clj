(ns clj-bittorrent.math.numbers
  (:require [schema.core :as schema]))

(defn nonneg? [x]
  (not (neg? x)))

(defn multiple-of? [x y]
  (= 0 (rem x y)))

(def NonNegativeInt
  "A number zero or greater."
  (schema/constrained schema/Int nonneg?))

;; I am aware that indexes, lengths, and counts are all the same thing,
;; but I like making a descriptive schema like this, even if it leads
;; to a little repetition.

(def Index
  "A non-negative number, in the valid range for indexing some collection."
  NonNegativeInt)

(def Length
  "A non-negative number that can represent an array's length."
  NonNegativeInt)

(def Count
  "A non-negative number that can represent a count of things."
  NonNegativeInt)
