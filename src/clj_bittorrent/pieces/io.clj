(ns clj-bittorrent.pieces.io
  (:require [clj-mmap.core :as mmap]))

(defn write-pieces
  "Write all data possessed by the client for torrent m to the filename at f."
  [m ps f]
  (with-open [mapped-file (mmap/get-mmap f)]
    (for [p ps
          :let [{:keys [index contents]} p
                piece-length (count contents)
                piece-pos (* index piece-length)]]
      (mmap/put-bytes mapped-file contents piece-pos))))
