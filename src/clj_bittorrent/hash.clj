(ns clj-bittorrent.hash
  "Hashing functions."
  (:require [schema.core :as schema]
            [clj-bittorrent.binary :as bin])
  (:import (java.security MessageDigest)))

(def Sha1Hash
  (schema/constrained bin/ByteArray #(= 20 (count %))))

(schema/defn sha1 :- Sha1Hash
  "Hash a byte array with SHA-1."
  [x :- bin/ByteArray]
  (.digest (MessageDigest/getInstance "sha1") (bytes x)))
