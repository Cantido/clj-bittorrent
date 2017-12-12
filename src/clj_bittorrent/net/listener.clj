(ns clj-bittorrent.net.listener
  (:require [clj-bittorrent.net.tcp :as tcp]
            [clj-bittorrent.message.state :as msgapply]
            [clj-bittorrent.message.decode :as msgdecode])
  (:import (java.io Reader Writer)
           (org.apache.commons.io IOUtils)))

(defn create-message-handler [peer]
  (fn [^Reader reader ^Writer writer]
    (msgapply/apply-msg
      (msgdecode/next-msg reader)
      peer)))

(defn build-server [peer]
  (tcp/start
    (tcp/tcp-server
      :port    5000
      :handler (-> peer
                   create-message-handler
                   tcp/wrap-io))))
