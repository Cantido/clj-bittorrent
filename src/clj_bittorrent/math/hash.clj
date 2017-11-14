(ns clj-bittorrent.math.hash
  "Hashing functions."
  (:require [schema.core :as schema]
            [clj-bittorrent.math.binary :as bin])
  (:import (java.security MessageDigest)))

(def Sha1Hash
  (schema/constrained bin/ByteArray #(= 20 (count %))))

(schema/defn sha1 :- Sha1Hash
  "Hash a byte array with SHA-1."
  [x :- bin/ByteArray]
  (seq (.digest (MessageDigest/getInstance "sha1") (byte-array x))))
