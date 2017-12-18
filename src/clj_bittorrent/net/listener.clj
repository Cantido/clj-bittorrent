(ns clj-bittorrent.net.listener
  (:require [clj-bittorrent.net.tcp :as tcp]
            [clj-bittorrent.message.state :as msgapply]
            [clj-bittorrent.message.decode :as msgdecode]
            [clj-bittorrent.message.handshake :as handshake]
            [clj-bittorrent.peer.connection :as conn]
            [clojure.java.io :as io])
  (:import (java.net Socket)))

(defn- new-connection-agent [socket hs]
  (-> conn/new-connection
      (assoc :socket socket)
      (conn/handshake hs)
      (agent :error-mode :continue)))

(defn- handle-handshake [session-agent socket]
  (let [connection
        (->> socket
          (handshake/listen @session-agent)
          (new-connection-agent socket))]
    (send session-agent update :connection conj connection)
    connection))

(defn- handle-next-message
  "Wait for the next message on the connection, dispatch it to the connection
  agent, then return."
  [connection]
  (->> (:socket connection)
       (io/reader)
       (msgdecode/next-msg)
       ;; By this point we have the entire message, so we don't need
       ;; to send-off. This will just update the connection
       ;; map with whatever data we got.
       (send connection msgapply/update-connection)))

(defn- handle-messages
  "Handle all incoming messages on the given socket until it is closed."
  [connection]
  (while (not (.isClosed (:socket connection)))
    (handle-next-message connection)))

(defn- connection-handler
  "Returns a function that handles incoming messages on behalf of a session,
  dispatching them to its agent, after performing a handshake."
  [session-agent]
  (fn [^Socket socket]
    (->> socket
      (handle-handshake session-agent)
      (handle-messages))))

(defn build-server
  "Create a TCP server that will accept connections for all peers
  in a session."
  [session-agent]
  (tcp/tcp-server
    :port (:port @session-agent)
    :handler (connection-handler session-agent)))
