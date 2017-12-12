(ns clj-bittorrent.net.listener
  (:require [clj-bittorrent.net.tcp :as tcp]
            [clj-bittorrent.message.state :as msgapply]
            [clj-bittorrent.message.decode :as msgdecode])
  (:import (java.io Reader Writer)))

(defn apply-next-msg
  "Applies the next message on reader to the given peer."
  [peer ^Reader reader ^Writer writer]
  (msgapply/apply-msg
    (msgdecode/next-msg reader)
    peer))

(defn handle-msg
  "Applies the next message on reader to the given agent.
  Since reading from the socket is always blocking, the
  message will always be handled by send-off."
  [peer-agent ^Reader reader ^Writer writer]
  (send-off
    peer-agent
    apply-next-msg
    reader
    writer))

(defn build-server
  "Create a TCP server that will accept messages to update an agent."
  [peer-agent]
  (tcp/tcp-server
    :port    5000
    :handler (tcp/wrap-io (partial handle-msg peer-agent))))
