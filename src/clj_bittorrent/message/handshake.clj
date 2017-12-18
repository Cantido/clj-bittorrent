(ns clj-bittorrent.message.handshake
  (:require [clj-bittorrent.math.binary :as bin])
  (:import (java.nio.charset StandardCharsets)))

(def ^:private pstr
  "string identifier of the protocol."
  (-> "BitTorrent protocol"
      (.getBytes StandardCharsets/US_ASCII)
      (seq)))

(def ^:private reserved
  "Bytes reserved for future versions of the protocol."
  (take 8 (repeat 0x00)))

(defn handshake
  "Assembles and returns a handshake packet for metainfo m."
  [m]
  {:pre  [(= 20 (count (:info-hash m)))
          (= 20 (count (:peer-id m)))]
   :post [(= 68 (count (seq %)))
          (every? bin/ubyte? %)]}
  (let [{:keys [info-hash
                peer-id]} m]
    (concat
      [(count pstr)]
      pstr
      reserved
      info-hash
      peer-id)))
