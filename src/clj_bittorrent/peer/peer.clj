(ns clj-bittorrent.peer.peer
  "Manipulate peer maps."
  (:require [clojure.set :as s]
            [schema.core :as schema]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.hash :as hash]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.net.net :as net]
            [clj-bittorrent.pieces.blocks :as blocks])
  (:import (java.nio.charset StandardCharsets)))

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
  {:choked                     Choked
   :interested                 Interested
   :have                       HasPieces
   :blocks                     HasBlocks
   :requested                  RequestedBlocks
   (schema/optional-key :port) net/Port})

(schema/def peer-default-state :- Peer
  {:choked true
   :interested false
   :have #{}
   :blocks #{}
   :requested #{}})

(schema/defn choke :- Peer
  "Choke the peer. The peer that is choked will be ignored
   until it is unchoked."
  [peer :- Peer]
  (assoc peer :choked true))

(schema/defn unchoke :- Peer
  "Unchoke the peer. The peer will no longer be ignored."
  [peer :- Peer]
  (assoc peer :choked false))

(schema/defn interested :- Peer
  "Mark the peer as interested. An interested peer wants something
   that other peers have to offer, and will begin requesting blocks."
  [peer :- Peer]
  (assoc peer :interested true))

(schema/defn not-interested :- Peer
  "Mark the peer as not interested. A peer that is not interested will
   not send requests for data to other peers."
  [peer :- Peer]
  (assoc peer :interested false))

(schema/defn un-request-blocks :- Peer
  ([peer :- Peer
    index-or-block]
   (let [index (if (number? index-or-block)
                 index-or-block
                 (:index index-or-block))]
     (update
       peer
       :requested
       #(blocks/remove-blocks-matching-indices % index)))))

(schema/defn has-piece :- Peer
  "Update the peer to say they have piece n. Also clears that piece
   from the set of requested blocks, since the peer has it already."
  ([peer :- Peer
    n :- n/Index]
   (-> peer
       (update :have #(conj (set %) n))
       (un-request-blocks n)))
  ([peer n & more]
   (reduce has-piece (has-piece peer n) more)))

(schema/defn request-block :- Peer
  "Add a request for a block to a peer. If the peer has that block in
  their \"have\" set, then that block is also removed from that set."
  [peer :- Peer
   block :- blocks/BlockId]
  {:post [(not (contains? (:have %) (:index block)))
          (contains? (:requested %) block)]}
  (-> peer
      (update :have #(disj (set %) (:index block)))
      (update :requested #(conj (set %) block))))

(schema/defn add-block :- Peer
  "Add a block of data to a peer. If the peer requested the given
   block, that block will be removed from the requested set as well."
  [peer :- Peer
   block :- blocks/BlockData]
  (-> peer
      (update
        :blocks
        #(blocks/conj-condense
           (set %)
           (select-keys block [:index :offset :contents])))
      (un-request-blocks block)))
