(ns clj-bittorrent.peer.schema
  (:require [clj-bittorrent.net.net :as net]
            [schema.core :as schema]
            [clj-bittorrent.pieces.blocks :as blocks]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.math.hash :as hash]))

(def PeerState
  "The position of the peer in the BitTorrent state machine"
  (schema/enum
    :start
    :waiting-for-handshake
    :choked
    :interested
    :choked-interested
    :ready))

(def PeerId
  "A unique identifier for a peer."
  hash/Sha1Hash)

(def Choked
  "A choked peer is not allowed to request data."
  schema/Bool)

(def Interested
  "An interested peer may start requesting blocks if it is unchoked."
  schema/Bool)

(def HasPieces
  "The pieces that a peer already has, zero-indexed."
  #{n/Index})

(def HasBlocks
  "The blocks (partial pieces) that a peer already has."
  #{blocks/BlockData})

(def RequestedBlocks
  "The IDs of blocks that a peer has requested."
  #{blocks/BlockId})

(def Peer
  "A peer is trying to download or upload pieces of a file."
  {:state                                PeerState
   :have                                 HasPieces
   :blocks                               HasBlocks
   :requested                            RequestedBlocks
   (schema/optional-key :pending-verify) HasBlocks
   (schema/optional-key :port)           net/Port})

(def Connection
  "A connection between the local peer (AKA \"client\") and a remote peer."
  {:client Peer
   :peer Peer})
