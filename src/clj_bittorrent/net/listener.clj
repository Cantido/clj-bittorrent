(ns clj-bittorrent.net.listener
  (:require [clj-bittorrent.net.tcp :as tcp]
            [clj-bittorrent.peer.message :as msg])
  (:import (java.io Reader Writer)
           (org.apache.commons.io IOUtils)))

(defn create-message-handler [peer]
  (fn [^Reader reader ^Writer writer]
    (msg/apply-msg
      (msg/read reader)
      peer)))

(defn build-server [peer]
  (tcp/start
    (tcp/tcp-server
      :port    5000
      :handler (-> peer
                   create-message-handler
                   tcp/wrap-io))))
