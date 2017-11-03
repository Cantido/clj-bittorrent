(ns clj-bittorrent.message
  (:require [clj-bittorrent.binary :as bin]
            [clojure.set :as sets]
            [clj-bittorrent.peer :as peer]))

(def msg-id
  {:keep-alive     nil
   :choke          0x00
   :unchoke        0x01
   :interested     0x02
   :not-interested 0x03
   :have           0x04
   :bitfield       0x05
   :request        0x06
   :piece          0x07
   :cancel         0x08
   :port           0x09})

(def msg
  (sets/map-invert msg-id))

(defn keep-alive
  "Builds a kee-alive message. Peers may close a connection if they receive
   no messages (keep-alive or any other message) for a certain period of time,
   so a keep-alive message must be sent to maintain the connection alive if no
   command have been sent for a given amount of time. This amount of time is
   generally two minutes."
  []
  [0x00 0x00 0x00 0x00])

(defn choke
  "Builds a choke message."
  []
  [0x00 0x00 0x00 0x01 (:choke msg-id)])

(defn unchoke []
  [0x00 0x00 0x00 0x01 (:unchoke msg-id)])

(defn interested
  "Builds an interested message."
  []
  [0x00 0x00 0x00 0x01 (:interested msg-id)])

(defn not-interested
  "Builds a not-interested message."
  []
  [0x00 0x00 0x00 0x01 (:not-interested msg-id)])

(defn have
  "Builds a have message. piece-index is a zero-based index of a piece that
   has just been successfully downloaded and verified via the hash. "
  [piece-index]
  {:pre [(<= piece-index Integer/MAX_VALUE)]
   :post [(= 9 (count %))]}
  (concat [0x00 0x00 0x00 0x05]
          [(:have msg-id)]
          (bin/pad-bytes 4 (bin/int-bytearray piece-index))))

(defn bitfield
  "Builds a bitfield message. The payload is a bitfield
   representing the pieces that have been successfully downloaded. The high
   bit in the first byte corresponds to piece index 0. Bits that are cleared
   indicated a missing piece, and set bits indicate a valid and available
   piece. Spare bits at the end are set to zero.

   The bitfield message may only be sent immediately after the handshaking
   sequence is completed, and before any other messages are sent. It is
   optional, and need not be sent if a client has no pieces."
  [bitfield]
  {:pre [(coll? bitfield)]
   :post [(= (count %) (+ 5 (count bitfield)))]}
  (concat (bin/int-byte-field 4 (inc (count bitfield)))
          [(:bitfield msg-id)]
          (byte-array bitfield)))


(defn request
  "Builds a request message. The request message is used to request a block.
   The index specifies the piece index, begin specifies the offset within
   the piece, and the length specifies how many bytes are requested.

   A common block size is 16 KB (2^14 KB)."
  [index begin length]
  {:pre [(bin/sint? index)
         (bin/sint? begin)
         (bin/sint? length)]
   :post [(= 17 (count %))]}
  (concat
    [0x00 0x00 0x00 13]
    [(:request msg-id)]
    (bin/int-byte-field 4 index)
    (bin/int-byte-field 4 begin)
    (bin/int-byte-field 4 length)))


(defn piece
  "Builds a piece message. A piece message contains part of a piece.
   The index specifies the index of the piece (zero-based). begin specifies
   the zero-based offset of the block within the piece. block contains the
   binary data of the piece."
  [index begin block]
  {:pre [(bin/sint? index)
         (bin/sint? begin)
         (bin/sint? (+ 9 (count block)))]
   :post [(= (+ 13 (count block) (count %)))]}
  (concat
    (bin/int-byte-field 4 (+ 9 (count block)))
    [(:piece msg-id)]
    (bin/int-byte-field 4 index)
    (bin/int-byte-field 4 begin)
    (seq block)))


(defn cancel
  "Builds a cancel message. The cancel message is used to cancel requests for
  blocks. The payload is identical to that of the \"request\" message."
  [index begin length]
  {:pre [(bin/sint? index)
         (bin/sint? begin)
         (bin/sint? length)]
   :post [(= 17 (count %))]}
  (concat
    [0x00 0x00 0x00 13]
    [(:cancel msg-id)]
    (bin/int-byte-field 4 index)
    (bin/int-byte-field 4 begin)
    (bin/int-byte-field 4 length)))


(defn port
  "Builds a port message. The port message is sent by newer versions of the
   Mainline that implements a DHT tracker. The listen port is the port this
   peer's DHT node is listening on. This peer should be inserted in the local
   routing table (if DHT tracker is supported)."
  [port]
  {:pre [(bin/fits-in-bytes-unsigned 2 port)]
   :post [(= 7 (count %))]}
  (concat [0x00 0x00 0x00 0x03]
          [(:port msg-id)]
          (bin/int-byte-field 2 port)))

(defn- split-message
  ([xs i j]
   (let [[a ys] (split-at i xs)
         [b c] (split-at j ys)]
     [a b c]))
  ([xs i j k]
   (let [[a ys] (split-at i xs)
         [b zs] (split-at j ys)
         [c d] (split-at k zs)]
     [a b c d]))
  ([xs i j k l]
   (let [[a ys] (split-at i xs)
         [b zs] (split-at j ys)
         [c ws] (split-at k zs)
         [d e] (split-at k ws)]
     [a b c d e])))

(defn- payload [xs]
  (nth (split-message xs 4 1) 2))

(defn- recv-have [xs]
  {:id :have
   :index (bin/int-from-bytes (payload xs))})

(defn- recv-bitfield [xs]
  {:id :bitfield
   :indices (bin/bitfield-set (payload xs))})

(defn- recv-request [xs]
  (merge {:id :request}
         (zipmap [:index :offset :length]
                 (drop 2 (map bin/int-from-bytes
                              (split-message xs 4 1 4 4))))))

(defn- recv-cancel [xs]
  (merge {:id :cancel}
         (zipmap [:index :offset :length]
                 (drop 2 (map bin/int-from-bytes
                              (split-message xs 4 1 4 4))))))

(defn- recv-piece [xs]
  (let [[len id index begin block] (split-message xs 4 1 4 4)]
    {:id :piece
     :index (bin/int-from-bytes index)
     :offset (bin/int-from-bytes begin)
     :block block}))

(defn- recv-port [xs]
  {:id :port :port (bin/int-from-bytes (payload xs))})

(defn- recv-type [xs]
  (get msg (get (vec xs) 4)))

(defmulti recv recv-type)

(defmethod recv :keep-alive     [x] {:id :keep-alive})
(defmethod recv :choke          [x] {:id :choke})
(defmethod recv :unchoke        [x] {:id :unchoke})
(defmethod recv :interested     [x] {:id :interested})
(defmethod recv :not-interested [x] {:id :not-interested})
(defmethod recv :have           [x] (recv-have x))
(defmethod recv :bitfield       [x] (recv-bitfield x))
(defmethod recv :request        [x] (recv-request x))
(defmethod recv :piece          [x] (recv-piece x))
(defmethod recv :cancel         [x] (recv-cancel x))
(defmethod recv :port           [x] (recv-port x))


(defn apply-type [x & more] (:id x))

(defmulti apply-msg
          "Act upon a message sent by a remote peer."
          apply-type)

(defmethod apply-msg :keep-alive     [msg state] state)
(defmethod apply-msg :choke          [msg state] (update-in state [:client] peer/choke))
(defmethod apply-msg :unchoke        [msg state] (update-in state [:client] peer/unchoke))
(defmethod apply-msg :interested     [msg state] (update-in state [:peer] peer/interested))
(defmethod apply-msg :not-interested [msg state] (update-in state [:peer] peer/not-interested))
(defmethod apply-msg :have           [msg state] (update-in state [:peer] #(peer/add-piece % (:index msg))))
(defmethod apply-msg :bitfield       [msg state] (update-in state [:peer] #(apply peer/add-piece % (:indices msg))))
(defmethod apply-msg :request        [msg state] (update-in state [:peer] #(peer/request % (select-keys msg[:index :offset :length]))))
(defmethod apply-msg :piece          [msg state] state)
(defmethod apply-msg :cancel         [msg state] state)
(defmethod apply-msg :port           [msg state] state)
