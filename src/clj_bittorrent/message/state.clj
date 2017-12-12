(ns clj-bittorrent.message.state
  (:require [clj-bittorrent.peer.connection :as c]
            [schema.core :as schema]
            [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.message.schema :refer :all]))

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
