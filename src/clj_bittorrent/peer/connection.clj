(ns clj-bittorrent.peer.connection
  "Manipulate client-peer connection maps."
  (:require [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.state :as fsm]))

(def connection-fsm
  {:waiting-for-handshake {:handshake :ready}})

(def ^:private next-state (fsm/entity-state-machine connection-fsm))

(def new-connection
  {:state :waiting-for-handshake
   :client peer/peer-default-state
   :peer   peer/peer-default-state})

(defn handshake [conn h]
  (-> conn
      (update :client peer/handshake)
      (update :peer peer/handshake)
      (next-state :handshake)
      (assoc-in conn [:peer :peer-id (:peer-id h)])))

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
