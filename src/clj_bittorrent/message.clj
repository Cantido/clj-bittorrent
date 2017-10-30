(ns clj-bittorrent.message
  (:require [clj-bittorrent.binary :as bin]))

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
  (clojure.set/map-invert msg-id))

(defn- have-message
  [piece-index]
  {:pre [(<= piece-index Integer/MAX_VALUE)]
   :post [(= 9 (count %))]}
  (concat [0x00 0x00 0x00 0x05]
          [(:have msg-id)]
          (bin/pad-bytes 4 (bin/int-bytearray piece-index))))

(defn- bitfield-message
  [bitfield]
  {:post [(<= 5 (count %))]}
  (concat (bin/pad-bytes 4 (inc (count bitfield)))
          [(:bitfield msg-id)]
          (byte-array bitfield)))

(defn- request-message
  [index begin length]
  (concat
    [0x00 0x00 0x00 13]
    [(:request msg-id)]
    (bin/int-byte-field 4 index)
    (bin/int-byte-field 4 begin)
    (bin/int-byte-field 4 length)))

(defn- piece-message
  [index begin block]
  (concat
    (bin/int-byte-field 4 (+ 9 (count block)))
    [(:piece msg-id)]
    (bin/int-byte-field 4 index)
    (bin/int-byte-field 4 begin)
    (seq block)))

(defn- cancel-message
  [index begin length]
  (concat
    [0x00 0x00 0x00 13]
    [(:cancel msg-id)]
    (bin/int-byte-field 4 index)
    (bin/int-byte-field 4 begin)
    (bin/int-byte-field 4 length)))

(defmulti message class)

(defmethod message :keep-alive     [] [0x00 0x00 0x00 0x00])
(defmethod message :choke          [] [0x00 0x00 0x00 0x01 (:choke msg-id)])
(defmethod message :unchoke        [] [0x00 0x00 0x00 0x01 (:unchoke msg-id)])
(defmethod message :interested     [] [0x00 0x00 0x00 0x01 (:interested msg-id)])
(defmethod message :not-interested [] [0x00 0x00 0x00 0x01 (:not-interested msg-id)])
(defmethod message :have           [piece-index] (have-message piece-index))
(defmethod message :bitfield       [bits] (bitfield-message bits))
(defmethod message :request        [index begin length] (request-message index begin length))
(defmethod message :piece          [index begin block] (piece-message index begin block))
(defmethod message :cancel         [index begin length] (cancel-message index begin length))
(defmethod message :port           [port] (concat [0x00 0x00 0x00 0x03 (:port msg-id)] (intfield 2 port)))

(defn- msg-type [xs]
  (get msg (get xs 4)))

(defmulti recv msg-type)

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
