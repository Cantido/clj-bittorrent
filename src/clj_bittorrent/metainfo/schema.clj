(ns clj-bittorrent.metainfo.schema
  "Schemas for metainfo data"
  (:require [clj-bittorrent.math.hash :as hash]
            [clj-bittorrent.math.numbers :as n]
            [schema.core :as schema]
            [clj-bittorrent.math.binary :as bin]))

(def InfoHash
  "An identifier for a torrent, calculated by taking the SHA-1 hash of the
   value in the \"info\" key of a metainfo file."
  hash/Sha1Hash)

(def SingleFileInfo
  "Describes a file in a single-file torrent metainfo file."
  {:length       n/Length
   :md5sum       schema/Str
   :name         schema/Str
   :piece-length n/Length
   :pieces       bin/ByteArray})

(def FileInfo
  "Describes a single file in a multi-file metainfo file."
  {:length n/Length
   :md5sum schema/Str
   :path   [schema/Str]})

(def MultiFileInfo
  "Describes all files contained in a multi-file metainfo file.."
  {:name         schema/Str
   :piece-length n/Length
   :pieces       bin/ByteArray})

(def Metainfo
  "Describes a single file or multiple files that can be downloaded from peers
   provided by provided announce server."
  {:announce      schema/Str
   :announce-list [schema/Str]
   :created-by    schema/Str
   :creation-date n/NonNegativeInt
   :encoding      schema/Str
   :info          (schema/conditional map? SingleFileInfo :else MultiFileInfo)})
