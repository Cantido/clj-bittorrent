(ns clj-bittorrent.peer
  "Manipulate peer maps."
  (:require [clj-bittorrent.binary :as bin]
            [clojure.set :as s])
  (:import (java.nio.charset StandardCharsets)))

(def peer-default-state
  {:choked true
   :interested false
   :pieces #{}
   :requested #{}})

(defn choke
  "Choke the peer. The peer that is choked will be ignored until it is unchoked."
  [peer]
  (assoc peer :choked true))

(defn unchoke
  "Unchoke the peer. The peer will no longer be ignored."
  [peer]
  (assoc peer :choked false))

(defn interested
  "Mark the peer as interested. An interested peer wants something that
   other peers have to offer, and will begin requesting blocks."
  [peer]
  (assoc peer :interested true))

(defn not-interested
  "Mark the peer as not interested. A peer that is not interested will not
   send requests for data to other peers."
  [peer]
  (assoc peer :interested false))

(defn remove-blocks-matching-indices [b & ns]
  (set (remove (comp (set ns)
                     :index)
               b)))

(defn add-piece [peer & ns]
  {:pre [(every? number? ns)]
   :post [(s/subset? ns (:pieces %))]}
  (-> peer
    (update :pieces #(s/union % (set ns)))
    (update :requested #(apply remove-blocks-matching-indices % ns))))

(defn request [peer block]
  {:pre [(not (neg? (:index block)))
         (not (neg? (:offset block)))
         (pos? (:length block))]
   :post [(not (contains? (:pieces %) (:index block)))
          (contains? (:requested %) block)]}
  (-> peer
    (update :pieces #(disj % (:index block)))
    (update :requested #(conj % block))))
