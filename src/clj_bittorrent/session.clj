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
   :seeders       {} ; keyed by tracker
   :leechers      {} ; keyed by tracker
   :intervals     {} ; keyed by tracker
   :min-intervals {} ; keyed by tracker
   :peers         {} ; keyed by peer id
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

(defn merge-tracker-response [s tracker-url response]
  (let
    [{:keys [peers
             complete
             incomplete
             interval
             min-interval]
      :or {complete 0
           incomplete 0
           min-interval 0}}
     response]
    (-> s
        (update-in [:seeders] #(assoc % tracker-url complete))
        (update-in [:leechers] #(assoc % tracker-url incomplete))
        (update-in [:interval] #(assoc % tracker-url interval))
        (update-in [:min-interval] #(assoc % tracker-url min-interval))
        (update-in [:peers] #(peers-map % peers)))))


(defn start
  "Start the download."
  [s http-client]
  (let [tracker-url (get-in s [:metainfo :announce])]
    (->
      (->> s
        announcement
        (tracker/announce http-client tracker-url)
        (merge-tracker-response s tracker-url))
      (next-state :start))))
