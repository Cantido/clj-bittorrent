(ns clj-bittorrent.peer.message
  "Encode and decode messages between peers."
  (:require [clojure.set :as sets]
            [schema.core :as schema]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.net.net :as net]
            [clj-bittorrent.peer.connection :as c]
            [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.pieces.blocks :as blocks])
  (:import (java.nio.charset StandardCharsets)
           (java.io Reader)))

;; Encoding a handshake message

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

;; Encoding messages

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
   :post [(= (+ 13 (count block))
             (count %))]}
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
  {:pre  [(bin/fits-in-bytes-unsigned? 2 port)]
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

;;;; Decoding messages

(def KeepAliveMessage
  {:id (schema/eq :keep-alive)})

(def ChokeMessage
  {:id (schema/eq :choke)})

(def UnchokeMessage
  {:id (schema/eq :unchoke)})

(def InterestedMessage
  {:id (schema/eq :interested)})

(def NotInterestedMessage
  {:id (schema/eq :not-interested)})

(def HaveMessage
  {:id (schema/eq :have)
   :index n/Index})

(def BitfieldMessage
  {:id (schema/eq :bitfield)
   :indices #{n/Index}})

(def RequestMessage
  {:id (schema/eq :request)
   :index n/Index
   :offset n/Index
   :length n/Length})

(def PieceMessage
  {:id (schema/eq :piece)
   :index n/Index
   :offset n/Index
   :contents [schema/Int]})

(def CancelMessage
  {:id (schema/eq :cancel)
   :index n/Index
   :offset n/Index
   :length n/Length})

(def PortMessage
  {:id (schema/eq :port)
   :port net/Port})


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
     :contents block}))

(defn- recv-port [xs]
  {:id :port :port (bin/int-from-bytes (payload xs))})

(defn- recv-type [xs]
  (get msg (get (vec xs) 4)))

(defmulti recv recv-type)

(declare x)

(schema/defmethod recv :keep-alive :- KeepAliveMessage
                  [x] {:id :keep-alive})

(schema/defmethod recv :choke :- ChokeMessage
                  [x] {:id :choke})

(schema/defmethod recv :unchoke :- UnchokeMessage
                  [x] {:id :unchoke})

(schema/defmethod recv :interested :- InterestedMessage
                  [x] {:id :interested})

(schema/defmethod recv :not-interested :- NotInterestedMessage
                  [x] {:id :not-interested})

(schema/defmethod recv :have :- HaveMessage
                  [x] (recv-have x))

(schema/defmethod recv :bitfield :- BitfieldMessage
                  [x] (recv-bitfield x))

(schema/defmethod recv :request :- RequestMessage
                  [x] (recv-request x))

(schema/defmethod recv :piece :- PieceMessage
                  [x] (recv-piece x))

(schema/defmethod recv :cancel :- CancelMessage
                  [x] (recv-cancel x))

(schema/defmethod recv :port :- PortMessage
                  [x] (recv-port x))

(defn read
  "Returns the next message off the reader"
  [^Reader reader]
  (let [lenbytes (repeatedly 4 #(.read reader))
        mlength (-> lenbytes
                    byte-array
                    bin/int-from-bytes)
        body (repeatedly mlength #(.read reader))]
    (recv (concat lenbytes body))))

(defn apply-type [x & more] (:id x))

(defmulti apply-msg
          "Act upon a message sent by a remote peer."
          apply-type)

(declare msg state)

(schema/defmethod apply-msg :keep-alive :- c/Connection
                  [msg :- KeepAliveMessage state :- c/Connection]
                  state)

(schema/defmethod apply-msg :choke :- c/Connection
                  [msg :- ChokeMessage state :- c/Connection]
                  (update-in state [:client] peer/choke))

(schema/defmethod apply-msg :unchoke :- c/Connection
                  [msg :- UnchokeMessage state :- c/Connection]
                  (update-in state [:client] peer/unchoke))

(schema/defmethod apply-msg :interested :- c/Connection
                  [msg :- InterestedMessage state :- c/Connection]
                  (update-in state [:peer] peer/interested))

(schema/defmethod apply-msg :not-interested :- c/Connection
                  [msg :- NotInterestedMessage state :- c/Connection]
                  (update-in state [:peer] peer/not-interested))

(schema/defmethod apply-msg :have :- c/Connection
                  [msg :- HaveMessage state :- c/Connection]
                  (update-in state [:peer] #(peer/has-piece % (:index msg))))

(schema/defmethod apply-msg :bitfield :- c/Connection
                  [msg :- BitfieldMessage state :- c/Connection]
                  (update-in
                    state
                    [:peer]
                    #(apply peer/has-piece % (:indices msg))))

(schema/defmethod apply-msg :request :- c/Connection
                  [msg :- RequestMessage state :- c/Connection]
                  (update-in
                    state
                    [:peer]
                    #(peer/request-block
                       %
                       (select-keys msg [:index :offset :length]))))

(schema/defmethod apply-msg :piece :- c/Connection
                  [msg :- PieceMessage state :- c/Connection]
                  (update-in
                    state
                    [:client]
                    #(peer/add-block
                       %
                       (select-keys msg [:index :offset :contents]))))

(schema/defmethod apply-msg :cancel :- c/Connection
                  [msg :- CancelMessage state :- c/Connection]
                  (update-in
                    state
                    [:peer :requested]
                    #(disj % (select-keys msg [:index :offset :length]))))

(schema/defmethod apply-msg :port :- c/Connection
                  [msg :- PortMessage state :- c/Connection]
                  (assoc-in state [:peer :port] (:port msg)))
