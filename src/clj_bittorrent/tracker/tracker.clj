(ns clj-bittorrent.tracker.tracker
  "Interact with tracker servers."
  (:require [clj-http.client :as client]
            [clojure.set :as s]
            [schema.core :as schema]
            [clj-bencode.core :as b]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.metainfo.metainfo :as metainfo]
            [clj-bittorrent.net.net :as net]
            [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.tracker.urlencode :as u])
  (:import (org.apache.http HttpRequest)))

(def FailureReason
  "A human-readable string containing an error message with the reason that
   an attempt to join a torrent swarm has failed."
  schema/Str)

(def Interval
  "The amount of time that a peer should wait between consecutive
  regular requests."
  n/NonNegativeInt)

(def CompleteCount
  "The number of online peers with complete copies of the torrent."
  n/Count)

(def IncompleteCount
  "The number of online peers downloading the torrent."
  n/Count)

(def IsCompact
  "If the client wants the peers list to be compressed into six-byte
   representations instead of the usual dict."
  (schema/enum 0 1))

(def Event
  "The state of the client's download."
  (schema/enum "started" "stopped" "completed"))

(def TrackerRequest
  "A request sent by a peer to get peers from a tracker, and to inform the
   tracker of its download status."
  {:info-hash metainfo/InfoHash
   :peer-id peer/PeerId
   :port     net/Port
   :uploaded n/Count
   :downloaded n/Count
   :left n/Count
   :compact IsCompact
   :event Event
   :ip schema/Str
   :numwant n/Count})

(def PeerResponse
  "One peer that can be contacted to download a file."
   {:ip       net/IpAddress
    :port     net/Port
    (schema/optional-key :peer-id) peer/PeerId})

(def CompactPeer
  "A peer response when Compact is set to 1"
  (schema/constrained bin/ByteArray #(= 6 (count %))))

(def CompactPeers
  "A collection of peer responses when Compact is set to 1"
  (schema/constrained bin/ByteArray #(n/multiple-of? (count %) 6)))

(def TrackerFailedResponse
  "The response when a request to join a swarm has failed."
  {:failure-reason FailureReason})

(def TrackerSuccessResponse
  "The response when a request to join a swarm has is successful."
  {:interval   Interval
   (schema/optional-key :min-interval) Interval
   :complete   CompleteCount
   :incomplete IncompleteCount
   :peers      [PeerResponse]})

(def TrackerResponse
  "The overall state of a BitTorrent network's download, including peers to
   contact to download it yourself. Could be an error message if there was a failure."
  (schema/conditional
    #(some? (get % :failure-reason)) TrackerFailedResponse
    :else TrackerSuccessResponse))

(def HttpRequestMap
  "The map required by clj-http to make a request"
  schema/Any)

(def HttpResponseMap
  "The map returned by clj-http"
  schema/Any)

(def response-kmap
  {"failure reason" :failure-reason
   "complete" :complete
   "downloaded" :downloaded
   "incomplete" :incomplete
   "interval" :interval
   "min interval" :min-interval
   "peers" :peers})

(schema/defn decode-peer-binary-entry :- PeerResponse
  [s :- CompactPeer]
  (let [[ip port] (split-at 4 s)]
    {:ip (bin/ipv4-address (seq (map bin/ubyte ip)))
     :port (BigInteger. (byte-array (seq port)))}))

(schema/defn decode-peers-binary :- [PeerResponse]
  [s :- CompactPeers]
  {:post [(every? map? %)
          (every? some? (map :ip %))
          (every? some? (map :port %))]}
  (->> s
       (seq)
       (partition 6)
       (map decode-peer-binary-entry)))

(schema/defn tracker-response :- TrackerResponse
  [m :- HttpResponseMap]
  {:pre [(some? m)]}
  (-> m
    (:body)
    (byte-array)
    (b/decode)
    (s/rename-keys response-kmap)
    (update :peers decode-peers-binary)))

(schema/defn tracker-request :- HttpRequestMap
  "Create an HTTP request map from the info in map m."
  [url :- net/Url
   m :- TrackerRequest]
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

(schema/defn announce :- TrackerResponse
  "Announce yourself to the tracker for torrent map m."
  [url :- net/Url
   m :- TrackerRequest]
  {:pre [(some? m)
         (= 20 (count (:info-hash m)))]}
  (->> m
    (tracker-request url)
    (client/request)
    (tracker-response)))

