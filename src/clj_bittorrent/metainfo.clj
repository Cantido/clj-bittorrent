(ns clj-bittorrent.metainfo
  (:require [clj-bencode.core :as b]
            [clojure.set :as s]
            [clojure.java.io :as io])
  (:import (org.apache.commons.io IOUtils)
           (java.security MessageDigest)
           (java.nio.charset StandardCharsets)))

(def metainfo-kmap {"announce" :announce
                    "created by" :created-by
                    "creation date" :creation-date
                    "encoding" :encoding
                    "info" :info})

(def info-kmap {"length" :length
                "name" :name
                "piece length" :piece-length
                "pieces" :pieces
                "private" :private})


;(defn sha1 [s]
;  (-> "sha1"
;      MessageDigest/getInstance
;      (.digest (bytes s))))


;(defn hash-next-piece
;  [piece-length s]
;  {:pre [(< 0 piece-length)]}
;  (let [buf (byte-array piece-length)
;        read-count (.read (io/input-stream s) buf)]
;    (cond
;      (= piece-length read-count) buf
;      (< 0 read-count piece-length) buf
;      (<= read-count 0) nil)))
;
;(defn pieces
;  [piece-length f]
;  {:pre [(< 0 piece-length) (some? f)]}
;  (let [stream (io/input-stream f)]
;    (take-while some? (repeatedly #(hash-next-piece piece-length f)))))

(def sha1-byte-count 20)

(defn expected-piece-count [m]
  (let [{:keys [info]} m]
    (let [{:keys [length piece-length]} info]
      (int (Math/ceil (float (/ length piece-length)))))))

(defn sha1-seq
  "Decodes a UTF-16 string into a seq of 20-byte SHA-1 byte-arrays."
  [piece-length coll]
  {:pre [(< 0 piece-length)]}
  (seq (map byte-array (partition sha1-byte-count (b/to-utf8 coll)))))

(defn encode-pieces
  "Updates the :pieces element in the array to be 20-byte byte
   arrays."
  [x]
  (let [piece-length (get-in x [:info :piece-length])]
    (update-in x [:info :pieces] (partial sha1-seq piece-length))))

(defn read-metainfo [x]
  (-> x
      (b/decode)
      (s/rename-keys metainfo-kmap)
      (update-in [:info] #(s/rename-keys % info-kmap))
      (update-in [:info :pieces] #(.getBytes (str %) StandardCharsets/US_ASCII))))
    ;(encode-pieces)))

