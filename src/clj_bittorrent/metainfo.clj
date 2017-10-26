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

(defn expected-piece-count [m]
  (let [{:keys [length piece-length]} (:info m)]
    (int (Math/ceil (float (/ length piece-length))))))

(defn read-metainfo [x]
  (-> x
      (b/decode)
      (s/rename-keys metainfo-kmap)
      (update-in [:info] #(s/rename-keys % info-kmap))
      (update-in [:info :pieces] (partial partition 20))))
