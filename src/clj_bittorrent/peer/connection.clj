(ns clj-bittorrent.peer.connection
  "Manipulate client-peer connection maps."
  (:require [clj-bittorrent.peer.peer :as peer]))

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
