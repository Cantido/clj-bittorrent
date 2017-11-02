(ns clj-bittorrent.peer
  "Interact with peers."
  (:require [clj-bittorrent.binary :as bin])
  (:import (java.nio.charset StandardCharsets)))

(def ^:private pstr
  "string identifier of the protocol."
  (-> "BitTorrent protocol"
    (.getBytes StandardCharsets/US_ASCII)
    (seq)))

(def ^:private pstrlen
  "length of protocol identifier"
  (count pstr))

(def ^:private reserved
  "Bytes reserved for future versions of the protocol."
  (take 8 (repeat 0x00)))

(defn handshake
  "Assembles and returns a handshake packet for m."
  [m]
  {:pre  [(= 20 (count (:info-hash m)))
          (= 20 (count (:peer-id m)))]
   :post [(= 68 (count (seq %)))
          (every? bin/ubyte? %)]}
  (let [{:keys [info-hash
                peer-id]} m]
    (concat
      (list pstrlen)
      pstr
      reserved
      info-hash
      peer-id)))

(def peer-default-state
  {:choked true
   :interested false})

(def connection-default-state
  {:client peer-default-state
   :peer   peer-default-state})

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

(defn transfer-allowed?
  [from to]
  (and (not (:choked from))
       (:interested to)))

(defn download-allowed?
  "Check if a download is allowed from the given peers."
  [connection]
  (transfer-allowed? (:peer connection) (:client connection)))

(defn upload-allowed?
  "Check if an upload is allowed from the given peers."
  [connection]
  (transfer-allowed? (:client connection) (:peer connection)))


