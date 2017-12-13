(ns clj-bittorrent.message.schema
  (:require [clojure.set :as set]
            [schema.core :as schema]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.net.net :as net]))

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

(def msg-type
  (set/map-invert msg-id))

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
  {:id    (schema/eq :have)
   :index n/Index})

(def BitfieldMessage
  {:id (schema/eq :bitfield)
   :indices #{n/Index}})

(def RequestMessage
  {:id     (schema/eq :request)
   :index  n/Index
   :offset n/Index
   :length n/Length})

(def PieceMessage
  {:id       (schema/eq :piece)
   :index    n/Index
   :offset   n/Index
   :contents [schema/Int]})

(def CancelMessage
  {:id     (schema/eq :cancel)
   :index  n/Index
   :offset n/Index
   :length n/Length})

(def PortMessage
  {:id   (schema/eq :port)
   :port net/Port})
