(ns clj-bittorrent.message.handshake
  (:require [clj-bittorrent.math.binary :as bin]
            [clojure.java.io :as io])
  (:import (java.nio.charset StandardCharsets)
           (java.io Writer Reader)
           (java.net Socket)))

(def ^:private pstr
  "string identifier of the protocol."
  (-> "BitTorrent protocol"
      (.getBytes StandardCharsets/US_ASCII)
      (seq)))

(def ^:private reserved
  "Bytes reserved for future versions of the protocol."
  (take 8 (repeat 0x00)))

(defn encode
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

(def handshake-length 68)

(defn decode [bs]
  {:pre [(= handshake-length (count bs))]
   :post [(= 19 (:protocol-name-length))
          (= "BitTorrent protocol" (:protocol %))]}
  (let [bs (vec bs)]
    {:protocol-name-length (bin/int-from-bytes (subvec bs 0 1))
     :protocol (String. (byte-array (subvec bs 1 19)))
     :reserved (subvec bs 19 27)
     :info-hash (subvec bs 27 47)
     :peer-id (subvec bs 47 67)}))

(defn read-handshake [^Reader reader]
  (decode (repeatedly handshake-length #(.read reader))))

(defn listen
  "Block on the reader waiting for a handshake, then respond with a
  handshake for the given session. Returns the remote peer's handshake."
  [m ^Socket socket]
  (let [^Reader reader (io/reader socket)
        ^Writer writer (io/writer socket)
        remote-handshake (read-handshake reader)]
    (.write writer (char-array (bin/sbytes-to-char-array (encode m))))
    remote-handshake))
