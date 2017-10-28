(ns clj-bittorrent.hash
  "Hashing functions."
  (:import (java.security MessageDigest)))

(defn sha1
  "Hash a byte array with SHA-1."
  [x]
  (-> (MessageDigest/getInstance "sha1")
      (.digest (bytes x))))
