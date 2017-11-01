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

(defn- have-message
  [piece-index]
  {:pre [(<= piece-index Integer/MAX_VALUE)]
   :post [(= 9 (count %))]}
  (concat [0x00 0x00 0x00 0x05]
          [(:have msg-id)]
          (bin/pad-bytes 4 (bin/int-bytearray piece-index))))

(defn- bitfield-message
  [bitfield]
  {:pre [(coll? bitfield)]
   :post [(= (count %) (+ 5 (count bitfield)))]}
  (concat (bin/int-byte-field 4 (inc (count bitfield)))
          [(:bitfield msg-id)]
          (byte-array bitfield)))

(defn- request-message
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

(defn- piece-message
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

(defn- cancel-message
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

(defn- port-message
  [port]
  {:pre [(bin/fits-in-bytes-unsigned 2 port)]
   :post [(= 7 (count %))]}
  (concat [0x00 0x00 0x00 0x03]
          [(:port msg-id)]
          (bin/int-byte-field 2 port)))

(defn- msg-type [x & more]
  x)

(defmulti message msg-type)

(defmethod message :keep-alive     [x] [0x00 0x00 0x00 0x00])
(defmethod message :choke          [x] [0x00 0x00 0x00 0x01 (:choke msg-id)])
(defmethod message :unchoke        [x] [0x00 0x00 0x00 0x01 (:unchoke msg-id)])
(defmethod message :interested     [x] [0x00 0x00 0x00 0x01 (:interested msg-id)])
(defmethod message :not-interested [x] [0x00 0x00 0x00 0x01 (:not-interested msg-id)])
(defmethod message :have           [x piece-index] (have-message piece-index))
(defmethod message :bitfield       [x bits] (bitfield-message bits))
(defmethod message :request        [x index begin length] (request-message index begin length))
(defmethod message :piece          [x index begin block] (piece-message index begin block))
(defmethod message :cancel         [x index begin length] (cancel-message index begin length))
(defmethod message :port           [x port] (port-message port))

(defn- recv-type [xs & more]
  (get msg (get xs 4)))

(defmulti recv recv-type)

(defmethod recv :keep-alive     [x] x)
(defmethod recv :choke          [x] x)
(defmethod recv :unchoke        [x] x)
(defmethod recv :interested     [x] x)
(defmethod recv :not-interested [x] x)
(defmethod recv :have           [x] x)
(defmethod recv :bitfield       [x] x)
(defmethod recv :request        [x] x)
(defmethod recv :piece          [x] x)
(defmethod recv :cancel         [x] x)
(defmethod recv :port           [x] x)


(defn apply-choke [msg state]
  (update-in state [:client] peer/choke))

(defn apply-unchoke [msg state]
  (update-in state [:client] peer/unchoke))

(defn apply-type [x & more]
  x)

(defn apply-no-msg [msg state])

(defmulti apply-msg apply-type)

(defmethod apply-msg :keep-alive     [msg-bytes state] (apply-no-msg (recv msg-bytes) state))
(defmethod apply-msg :choke          [msg-bytes state] state)
(defmethod apply-msg :unchoke        [msg-bytes state] state)
(defmethod apply-msg :interested     [msg-bytes state] state)
(defmethod apply-msg :not-interested [msg-bytes state] state)
(defmethod apply-msg :have           [msg-bytes state] state)
(defmethod apply-msg :bitfield       [msg-bytes state] state)
(defmethod apply-msg :request        [msg-bytes state] state)
(defmethod apply-msg :piece          [msg-bytes state] state)
(defmethod apply-msg :cancel         [msg-bytes state] state)
(defmethod apply-msg :port           [msg-bytes state] state)
