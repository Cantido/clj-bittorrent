(ns clj-bittorrent.peer.connection
  "Manipulate client-peer connection maps."
  (:require [clj-bittorrent.peer.peer :as peer]))

(def Connection
  "A connection between the local peer (AKA \"client\") and a remote peer."
  {:client peer/Peer
   :peer peer/Peer})

(def connection-default-state
  {:client peer/peer-default-state
   :peer   peer/peer-default-state})

(defn transfer-allowed?
  [from to]
  (and (not (= :choked (:state from)))
       (= :interested (:state to))))

(defn download-allowed?
  "Check if a download is allowed from the given peers."
  [connection]
  (transfer-allowed? (:peer connection) (:client connection)))

(defn upload-allowed?
  "Check if an upload is allowed from the given peers."
  [connection]
  (transfer-allowed? (:client connection) (:peer connection)))
