(ns clj-bittorrent.message.decode
  (:require [clj-bittorrent.message.schema :refer :all]
            [clj-bittorrent.math.binary :as bin]
            [schema.core :as schema])
  (:import (java.io Reader)))

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
     :contents block}))

(defn- recv-port [xs]
  {:id :port :port (bin/int-from-bytes (payload xs))})

(defn- recv-type [xs]
  (get msg-type (get (vec xs) 4)))

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
