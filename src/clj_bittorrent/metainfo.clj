(ns clj-bittorrent.metainfo
  "Extracts information from a metainfo (AKA .torrent) file."
  (:refer-clojure :exclude [read])
  (:require [clj-bencode.core :as b]
            [clojure.set :as s]
            [clj-bittorrent.binary :as bin]
            [clj-bittorrent.hash :as hash]
            [schema.core :as schema]
            [clj-bittorrent.numbers :as n]))

(def SingleFileInfo
  "Describes a file in a single-file torrent metainfo file."
  {:length n/Length
   :md5sum schema/Str
   :name schema/Str
   :piece-length n/Length
   :pieces [schema/Int]})

(def FileInfo
  "Describes a single file in a multi-file metainfo file."
  {:length n/Length
   :md5sum schema/Str
   :path [schema/Str]})

(def MultiFileInfo
  "Describes all files contained in a multi-file metainfo file.."
  {:name schema/Str
   :piece-length n/Length
   :pieces [schema/Int]})

(def Metainfo
  "Describes a single file or multiple files that can be downloaded from peers
   provided by provided announce server."
  {:announce      schema/Str
   :announce-list [schema/Str]
   :created-by    schema/Str
   :creation-date n/NonNegativeInt
   :encoding      schema/Str
   :info (schema/conditional map? SingleFileInfo :else MultiFileInfo)})

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
   be in the torrent file with metainfo m."
  [m]
  (let [{:keys [length piece-length]} (:info m)]
    (-> (/
          (int length)
          (int piece-length))
        (Math/ceil)
        (int))))

(defn- calc-info-hash [m]
  (map bin/ubyte (hash/sha1 (b/encode (:info m)))))

(defn read
  "Decode a BitTorrent metainfo file, AKA a .torrent file.
   Common keys are made into keywords, but uncommon or
   non-standard keys will remain as strings."
  [x]
  {:post [(= 20 (count (:info-hash %)))]}
  (let [original (b/decode x)]
    (-> original
        (s/rename-keys metainfo-kmap)
        (update-in [:info] #(s/rename-keys % info-kmap))
        (update-in [:info :pieces] (partial map bin/ubyte))
        (update-in [:info :pieces] (partial partition 20))
        (update-in [:info :files] (partial map rename-file-keys))
        (assoc :info-hash (calc-info-hash original)))))
