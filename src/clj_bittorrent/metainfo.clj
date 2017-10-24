(ns clj-bittorrent.metainfo
  (:require [clj-bencode.core :as b]
            [clojure.set :as s])
  (:import (org.apache.commons.io IOUtils)))

(def metainfo-kmap {"announce" :announce
                    "created by" :created-by
                    "creation date" :creation-date
                    "encoding" :encoding
                    "info" :info})

(def piece-kmap {"length" :length
                 "name" :name
                 "piece length" :piece-length
                 "pieces" :pieces
                 "private" :private})

(defn read-metainfo [x]
  (-> x
    (b/decode)
    (s/rename-keys metainfo-kmap)
    (update-in [:info] #(s/rename-keys % piece-kmap))))
