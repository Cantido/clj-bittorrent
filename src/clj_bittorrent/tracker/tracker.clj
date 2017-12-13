(ns clj-bittorrent.tracker.tracker
  "Interact with tracker servers."
  (:require [clj-http.client :as client]
            [clojure.set :as s]
            [schema.core :as schema]
            [clj-bencode.core :as b]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.metainfo.schema :as mschema]
            [clj-bittorrent.net.net :as net]
            [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.peer.schema :as pschema]
            [clj-bittorrent.tracker.urlencode :as u]
            [clj-bittorrent.tracker.schema :as tschema])
  (:import (org.apache.http HttpRequest)))


(def response-kmap
  {"failure reason" :failure-reason
   "complete" :complete
   "downloaded" :downloaded
   "incomplete" :incomplete
   "interval" :interval
   "min interval" :min-interval
   "peers" :peers})

(schema/defn decode-peer-binary-entry :- tschema/PeerResponse
  [s :- tschema/CompactPeer]
  (let [[ip port] (split-at 4 s)]
    {:ip (bin/ipv4-address (seq (map bin/ubyte ip)))
     :port (BigInteger. (byte-array (seq port)))}))

(schema/defn decode-peers-binary :- [tschema/PeerResponse]
  [s :- tschema/CompactPeers]
  {:post [(every? map? %)
          (every? some? (map :ip %))
          (every? some? (map :port %))]}
  (->> s
       (seq)
       (partition 6)
       (map decode-peer-binary-entry)))

(schema/defn tracker-response :- tschema/TrackerResponse
  [m :- tschema/HttpResponseMap]
  {:pre [(some? m)]}
  (-> m
    (:body)
    (byte-array)
    (b/decode)
    (s/rename-keys response-kmap)
    (update :peers decode-peers-binary)))

(schema/defn tracker-request :- tschema/HttpRequestMap
  "Create an HTTP request map from the info in map m."
  [url :- net/Url
   m :- tschema/TrackerRequest]
  ;; clj-http will url-encode our query params for us,
  ;; but it encodes UTF-16, which trackers like opentracker
  ;; don't like. They expect UTF-8.
  {:method :get
   :url (str url
             "?info_hash=" (u/urlencode (:info-hash m))
             "&peer_id=" (u/urlencode (:peer-id m)))
   :query-params (select-keys m [:port
                                 :uploaded
                                 :downloaded
                                 :left
                                 :compact
                                 :event
                                 :ip
                                 :numwant])})

(schema/defn announce :- tschema/TrackerResponse
  "Announce yourself to the tracker for torrent map m."
  [url :- net/Url
   m :- tschema/TrackerRequest]
  {:pre [(some? m)
         (= 20 (count (:info-hash m)))]}
  (->> m
    (tracker-request url)
    (client/request)
    (tracker-response)))
