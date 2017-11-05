(ns clj-bittorrent.tracker
  "Interact with tracker servers."
  (:require [clj-http.client :as client]
            [clj-bencode.core :as b]
            [clj-bittorrent.urlencode :as u]
            [clj-bittorrent.binary :as bin]
            [clj-bittorrent.net :as net]
            [clj-bittorrent.numbers :as n]
            [schema.core :as schema]
            [clj-bittorrent.peer :as peer]))

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
  "The number of online peers downloading the torrent.")

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
  {:port     net/Port
   :uploaded n/Count
   :downloaded n/Count
   :left n/Count
   :compact IsCompact
   :event Event
   :ip schema/Str
   :numwant n/Count})

(def PeerResponse
  "One peer that can be contacted to download a file."
  {"peer id" peer/PeerId
   "ip"      net/IpAddress
   "port"    net/Port})

(def TrackerResponse
  "The overall state of a BitTorrent network's download, including peers to
   contact to download it yourself."
  {"failure reason" FailureReason
   "interval" Interval
   "complete" CompleteCount
   "incomplete" IncompleteCount
   "peers" [PeerResponse]})

(defn- decode-peer-binary-entry [s]
  (let [[ip port] (split-at 4 s)]
    {:ip (bin/ipv4-address (seq (map bin/ubyte ip)))
     :port (BigInteger. (byte-array (seq port)))}))

(defn- decode-peers-binary
  [s]
  {:post [(every? map? %)
          (every? some? (map :ip %))
          (every? some? (map :port %))]}
  (->> s
       (seq)
       (partition 6)
       (map decode-peer-binary-entry)))

(defn- tracker-response
  [m]
  {:pre [(some? m)]}
  (-> m
    (:body)
    (byte-array)
    (b/decode)
    (update-in ["peers"] decode-peers-binary)))

(defn- tracker-request
  "Create an HTTP request map from the info in map m."
  [url m]
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

(defn announce
  "Announce yourself to the tracker for torrent map m."
  [url m]
  {:pre [(some? m)
         (= 20 (count (:info-hash m)))]}
  (->> m
    (tracker-request url)
    (client/request)
    (tracker-response)))

