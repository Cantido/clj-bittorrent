(ns clj-bittorrent.peer
  "Manipulate peer maps."
  (:require [clj-bittorrent.binary :as bin]
            [clj-bittorrent.blocks :as blocks]
            [clojure.set :as s]
            [schema.core :as schema]
            [clj-bittorrent.numbers :as n])
  (:import (java.nio.charset StandardCharsets)))

(def PeerId
  "A unique identifier for a peer."
  (schema/constrained [bin/SignedByte] #(= 20 (count %))))

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
  {:choked     Choked
   :interested Interested
   :have       HasPieces
   :blocks     HasBlocks
   :requested  RequestedBlocks})

(def peer-default-state
  {:choked true
   :interested false
   :have #{}
   :blocks #{}
   :requested #{}})

(defn choke
  "Choke the peer. The peer that is choked will be ignored until it is unchoked."
  [peer]
  (assoc peer :choked true))

(defn unchoke
  "Unchoke the peer. The peer will no longer be ignored."
  [peer]
  (assoc peer :choked false))

(defn interested
  "Mark the peer as interested. An interested peer wants something that
   other peers have to offer, and will begin requesting blocks."
  [peer]
  (assoc peer :interested true))

(defn not-interested
  "Mark the peer as not interested. A peer that is not interested will not
   send requests for data to other peers."
  [peer]
  (assoc peer :interested false))

(defn- index? [x]
  (and (number? x)
       (not (neg? x))))

(defn- un-request-blocks
  ([peer index-or-block]
   (let [index (if (number? index-or-block)
                 index-or-block
                 (:index index-or-block))]
     (update peer :requested #(blocks/remove-blocks-matching-indices % index)))))

(defn has-piece
  "Update the peer to say they have piece n. Also clears that piece from
   the set of requested blocks, since the peer has it already."
  ([peer n]
   {:pre [(index? n)]
    :post [(contains? (:have %) n)]}
   (-> peer
       (update :have #(conj (set %) n))
       (un-request-blocks n)))
  ([peer n & ns]
   (reduce has-piece (has-piece peer n) ns)))

(defn request-block
  "Add a request for a block to a peer. If the peer has that block in their
   \"have\" set, then that block is also removed from that set."
  [peer block]
  {:post [(not (contains? (:have %) (:index block)))
          (contains? (:requested %) block)]}
  (-> peer
      (update :have #(disj (set %) (:index block)))
      (update :requested #(conj (set %) block))))

(defn add-block
  "Add a block of data to a peer. If the peer requested the given block, that
   block will be removed from the requested set as well."
  [peer block]
  (-> peer
      (update :blocks #(blocks/conj-condense (set %)
                                             (select-keys block [:index :offset :contents])))
      (un-request-blocks block)))

