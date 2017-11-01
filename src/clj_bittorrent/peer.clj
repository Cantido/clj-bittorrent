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

(def connection-start-state
  {:choking true
   :interested false})

(def start-state
  {:client connection-start-state
   :peer connection-start-state})

(defn choke [m]
  (assoc m :choking true))

(defn unchoke [m]
  (assoc m :choking false))

(defn download? [m]
  (and (get-in m [:client :interested])
       (not (get-in m [:peer :choking]))))

(defn upload? [m]
  (and (not (get-in m [:client :choking]))
       (get-in m [:peer :interested])))
