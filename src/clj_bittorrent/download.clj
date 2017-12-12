(ns clj-bittorrent.download
  (:require [clj-bittorrent.state :as fsm]
            [clj-bittorrent.tracker.tracker :as tracker]
            [clj-bittorrent.net.net :as net]))

(def download-states
  {:start
     {:select-port :ready}

   :ready
     {:announce-started :started}

   :started
     {:select-pieces :started
      :select-peers :started
      :announce-completed :completed
      :announce-stopped :stopped}})

(def download-fsm (fsm/fsm download-states))

(defn- next-state
  [peer transition]
  {:pre [(:state peer) transition]
   :post [(:state %)]}
  (update-in peer [:state] #(download-fsm % transition)))

(def download-start-state
  {:state         :start
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

(defn announcement [dl]
  (let [{:keys [metainfo
                peer-id
                port
                uploaded
                downloaded
                left
                ip]} dl
        info-hash (:info-hash metainfo)]
      {:info-hash info-hash
       :peer-id peer-id
       :port port
       :uploaded uploaded
       :downloaded downloaded
       :left left
       :compact 1
       :event "started"
       :ip ip}))

(defn add-keyed [key m x]
  (assoc m (get x key) x))

(defn reduce-keyed [k m xs]
  (reduce
    (partial add-keyed k)
    m
    xs))

(def reduce-peers
  (partial reduce-keyed :peer-id))

(defn announce-start [dl]
  (let [tracker-url [:metainfo :announce]
        response
        (tracker/announce
          (get-in dl tracker-url)
          (announcement dl))
        {:keys [peers
                complete
                incomplete
                interval
                min-interval]
         :or {complete 0
              incomplete 0
              min-interval 0}}
        response]
    (-> dl
      (update-in [:seeders] #(assoc % tracker-url complete))
      (update-in [:leechers] #(assoc % tracker-url incomplete))
      (update-in [:interval] #(assoc % tracker-url interval))
      (update-in [:min-interval] #(assoc % tracker-url min-interval))
      (update-in [:peers] #(reduce-peers % (:peers response)))
      (next-state :announce-started))))
