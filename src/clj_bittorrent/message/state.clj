(ns clj-bittorrent.message.state
  (:require [clj-bittorrent.peer.schema :as pschema]
            [schema.core :as schema]
            [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.message.schema :refer :all]))

(defn apply-type [x & more] (:id x))

(defmulti apply-msg
          "Act upon a message sent by a remote peer and update a connection."
          apply-type)

(defn update-connection
  "apply-msg with flipped-around arguments."
  [c m]
  (apply-msg m c))

(declare msg state)

(schema/defmethod apply-msg :keep-alive :- pschema/Connection
                  [msg :- KeepAliveMessage state :- pschema/Connection]
                  state)

(schema/defmethod apply-msg :choke :- pschema/Connection
                  [msg :- ChokeMessage state :- pschema/Connection]
                  (update-in state [:client] peer/choke))

(schema/defmethod apply-msg :unchoke :- pschema/Connection
                  [msg :- UnchokeMessage state :- pschema/Connection]
                  (update-in state [:client] peer/unchoke))

(schema/defmethod apply-msg :interested :- pschema/Connection
                  [msg :- InterestedMessage state :- pschema/Connection]
                  (update-in state [:peer] peer/interested))

(schema/defmethod apply-msg :not-interested :- pschema/Connection
                  [msg :- NotInterestedMessage state :- pschema/Connection]
                  (update-in state [:peer] peer/not-interested))

(schema/defmethod apply-msg :have :- pschema/Connection
                  [msg :- HaveMessage state :- pschema/Connection]
                  (update-in state [:peer] #(peer/has-piece % (:index msg))))

(schema/defmethod apply-msg :bitfield :- pschema/Connection
                  [msg :- BitfieldMessage state :- pschema/Connection]
                  (update-in
                    state
                    [:peer]
                    #(apply peer/has-piece % (:indices msg))))

(schema/defmethod apply-msg :request :- pschema/Connection
                  [msg :- RequestMessage state :- pschema/Connection]
                  (update-in
                    state
                    [:peer]
                    #(peer/request-block
                       %
                       (select-keys msg [:index :offset :length]))))

(schema/defmethod apply-msg :piece :- pschema/Connection
                  [msg :- PieceMessage state :- pschema/Connection]
                  (update-in
                    state
                    [:client]
                    #(peer/add-block
                       %
                       (select-keys msg [:index :offset :contents]))))

(schema/defmethod apply-msg :cancel :- pschema/Connection
                  [msg :- CancelMessage state :- pschema/Connection]
                  (update-in
                    state
                    [:peer :requested]
                    #(disj % (select-keys msg [:index :offset :length]))))

(schema/defmethod apply-msg :port :- pschema/Connection
                  [msg :- PortMessage state :- pschema/Connection]
                  (assoc-in state [:peer :port] (:port msg)))
