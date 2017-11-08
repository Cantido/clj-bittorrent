(ns clj-bittorrent.net.net
  (:require [schema.core :as schema]
            [clj-bittorrent.math.numbers :as n]))

(defn port?
  "Returns true if x is a number within the range of valid port numbers."
  [x]
  (and (number? x)
       (<= 1 x 65535)))

(def Port
  "A network port number."
  (schema/constrained schema/Int port?))

(def IpAddress
  "An IPv4 or IPv6 address."
  schema/Str)

(def Url
  "A URL"
  schema/Str)
