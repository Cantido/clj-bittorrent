(ns clj-bittorrent.metainfo
  (:require [clj-bencode.core :as b]
            [clojure.set :as s])
  (:import (org.apache.commons.io IOUtils)))

(def kmap {"announce" :announce})

(defn read-metainfo [x]
  (-> x
    (b/decode)
    (s/rename-keys kmap)))

