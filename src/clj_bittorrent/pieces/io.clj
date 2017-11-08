(ns clj-bittorrent.pieces.io
  (:require [clj-mmap :as mmap]))

(defn write-pieces
  "Write all data possessed by the client for torrent m to the filename at f."
  [m client f]
  (with-open [mapped-file (mmap/get-mmap f)]
    (for [p (:pieces client)
          :let [{:keys [index offset contents]} p
                {:keys [piece-length]} m
                piece-pos (* index piece-length)]
          :when (= (count contents) piece-length)]
      (mmap/put-bytes mapped-file contents piece-pos))))
