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
