(ns clj-bittorrent.session
  (:require [clj-bittorrent.state :as fsm]
            [clj-bittorrent.tracker.tracker :as tracker]
            [clj-bittorrent.net.net :as net]))

(def session-fsm
  {:stopped
     {:start :started}

   :started
     {:complete :completed
      :stop :stopped}})

(def ^:private next-state (fsm/entity-state-machine session-fsm))

(def new-session
  {:state         :stopped
   :peer-id       0
   :port          0
   :uploaded      0
   :downloaded    0
   :left          0
   :local-peer    {}
   :announcements #{} ; relation of tracker-responses
   :peers         #{} ; relation of peer maps
   :ip            (net/ip-address)
   :metainfo      {}
   :connections   #{}})

(defn announcement [s]
  (-> {}
      (assoc :tracker-url (get-in s [:metainfo :announce]))
      (assoc :info-hash (get-in s [:metainfo :info-hash]))
      (merge (select-keys s #{:peer-id :port :uploaded :downloaded :left :ip}))
      (assoc :event "started")
      (assoc :compact 1)))

(defn peers-map
  ([peers]
   (peers-map {} peers))
  ([init peers]
   (reduce-kv
     (fn [i k v] (assoc i k (first v)))
     init
     (group-by :peer-id peers))))

(defn merge-tracker-response
  [s tracker-url response]
  (-> s
      (update-in [:announcements] #(conj % response))
      (update-in [:peers] #(peers-map % (:peers response)))))

(defn announce [s]
  (tracker/announce
    (get-in s [:metainfo :announce])
    (announcement s)))

(defn start
  "Start the download."
  [s]
  (next-state s :start))
