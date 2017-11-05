(ns clj-bittorrent.numbers
  (:require [schema.core :as schema]))

(defn nonneg? [x]
  (not (neg? x)))

(def NonNegativeInt
  (schema/constrained schema/Int nonneg?))

(def Index NonNegativeInt)

(def Length NonNegativeInt)

(def Count NonNegativeInt)
