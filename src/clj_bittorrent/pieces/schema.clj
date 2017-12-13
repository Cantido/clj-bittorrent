(ns clj-bittorrent.pieces.schema
  (:require [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.numbers :as n]))

(def BlockData
  "A piece is made up of several blocks of binary data."
  {:index    n/Index
   :offset   n/Index
   :contents bin/ByteArray})

(def BlockId
  "A piece is made up of several blocks of binary data."
  {:index  n/Index
   :offset n/Index
   :length n/Length})
