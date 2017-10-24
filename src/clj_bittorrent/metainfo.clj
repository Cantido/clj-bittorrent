(ns clj-bittorrent.metainfo
  (:require [clojure.java.io :as io]
            [clj-bencode.core :as b])
  (:import (org.apache.commons.io IOUtils)
           (java.nio ByteBuffer)
           (java.nio.charset StandardCharsets)))

(def kmap {"announce" :announce})

(defn read-metainfo [x]
  (-> x
    (b/decode)
    (clojure.set/rename-keys kmap)))

