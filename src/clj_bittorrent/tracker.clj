(ns clj-bittorrent.tracker
  "Interact with tracker servers."
  (:require [clj-http.client :as client]
            [clj-bencode.core :as b]
            [clj-bittorrent.urlencode :as u]
            [clj-bittorrent.binary :as bin]))


(defn- tracker-get [url m]
  (client/request
    {:method :get
     :url (str url
                "?info_hash=" (u/urlencode (:info-hash m))
                "&peer_id=" (u/urlencode (:peer-id m)))
     :query-params (dissoc m :info-hash :peer-id)}))

(defn announce
  "Announce yourself to the tracker for torrent map m."
  [url m]
  {:pre [(= 20 (count (:info-hash m)))]}
  (-> (tracker-get url m)
      (:body)
      (str)
      (.getBytes)
      (b/decode)))

