(ns clj-bittorrent.hash
  "Hashing functions."
  (:import (java.security MessageDigest)))

(defn sha1
  "Hash a byte array with SHA-1."
  [x]
  (.digest (MessageDigest/getInstance "sha1") (bytes x)))
