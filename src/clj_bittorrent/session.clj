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
  (let [{:keys [metainfo
                peer-id
                port
                uploaded
                downloaded
                left
                ip]} s
        info-hash (:info-hash metainfo)]
      {:tracker-url (:announce metainfo)
       :info-hash info-hash
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

(defn announce-start [s]
  (let [tracker-url [:metainfo :announce]
        response
        (tracker/announce
          (get-in s tracker-url)
          (announcement s))]))

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
        (update-in [:peers] #(reduce-peers % (:peers response))))))


(defn start
  "Start the download."
  [s]
  (let [tracker-url (get-in s [:metainfo :announce])]
    (->
      (->> s
        announcement
        (tracker/announce tracker-url)
        (merge-tracker-response s tracker-url))
      (next-state :start))))
