(ns clj-bittorrent.tracker.schema
  (:require [schema.core :as schema]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.metainfo.schema :as mschema]
            [clj-bittorrent.peer.schema :as pschema]
            [clj-bittorrent.net.net :as net]
            [clj-bittorrent.math.binary :as bin]))

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
  {:info-hash  mschema/InfoHash
   :peer-id    pschema/PeerId
   :port       net/Port
   :uploaded   n/Count
   :downloaded n/Count
   :left       n/Count
   :compact    IsCompact
   :event      Event
   :ip         schema/Str
   :numwant    n/Count})

(def PeerResponse
  "One peer that can be contacted to download a file."
  {:ip                            net/IpAddress
   :port                          net/Port
   (schema/optional-key :peer-id) pschema/PeerId})

(def CompactPeer
  "A peer response when Compact is set to 1"
  (schema/constrained bin/ByteArray #(= 6 (count %))))

(def CompactPeers
  "A collection of peer responses when Compact is set to 1"
  (schema/constrained bin/ByteArray #(n/multiple-of? (count %) 6)))

(def TrackerFailedResponse
  "The response when a request to join a swarm has failed."
  {:failure-reason FailureReason
   :timestamp      n/Timestamp})

(def TrackerSuccessResponse
  "The response when a request to join a swarm has is successful."
  {:interval                           Interval
   (schema/optional-key :min-interval) Interval
   :complete                           CompleteCount
   :incomplete                         IncompleteCount
   :peers                              [PeerResponse]
   :timestamp                          n/Timestamp})

(def TrackerResponse
  "The overall state of a BitTorrent network's download, including
  peers to contact to download it yourself. Could be an error message
  if there was a failure."
  (schema/conditional
    #(some? (get % :failure-reason)) TrackerFailedResponse
    :else TrackerSuccessResponse))

(def HttpRequestMap
  "The map required by clj-http to make a request"
  schema/Any)

(def HttpResponseMap
  "The map returned by clj-http"
  schema/Any)
