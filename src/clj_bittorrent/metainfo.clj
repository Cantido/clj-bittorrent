(ns clj-bittorrent.metainfo
  "Extracts information from a metainfo (AKA .torrent) file."
  (:require [clj-bencode.core :as b]
            [clojure.set :as s]))

(def ^:private metainfo-kmap
  {"announce" :announce
   "announce-list" :announce-list
   "created by" :created-by
   "creation date" :creation-date
   "encoding" :encoding
   "info" :info})

(def ^:private info-kmap
  {"files" :files
   "length" :length
   "name" :name
   "piece length" :piece-length
   "pieces" :pieces
   "private" :private})

(def ^:private file-kmap
  {"length" :length
   "md5sum" :md5sum
   "path" :path})

(defn- rename-file-keys [m]
  (s/rename-keys m file-kmap))

(defn expected-piece-count
  "Returns the number of :piece-length sized pieces that should
   be in the torrent file with metainfo m."[m]
  (let [{:keys [length piece-length]} (:info m)]
    (-> (/
          (int length)
          (int piece-length))
        (Math/ceil)
        (int))))

(defn read
  "Decode a BitTorrent metainfo file, AKA a .torrent file.
   Common keys are made into keywords, but uncommon or
   non-standard keys will remain as strings."
  [x]
  (-> x
      (b/decode)
      (s/rename-keys metainfo-kmap)
      (update-in [:info] #(s/rename-keys % info-kmap))
      (update-in [:info :pieces] (partial partition 20))
      (update-in [:info :files] (partial map rename-file-keys))))
