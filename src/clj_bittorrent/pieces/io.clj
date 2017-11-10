(ns clj-bittorrent.pieces.io
  (:require [clj-bittorrent.mmap :as mmap]))

(defn write-pieces
  "Write all data possessed by the client for torrent m to the filename at f."
  [m ps f]
  (let [piece-length (:piece-length m)
        piece-count (count (:pieces m))
        file-size (* piece-count piece-length)]
    (with-open [mapped-file (mmap/get-mmap f :read-write file-size)]
      (doseq [p ps
              :let [{:keys [index contents]} p
                    piece-pos (* index piece-length)]]
        (mmap/put-bytes
          mapped-file
          (byte-array contents)
          piece-pos)))))
