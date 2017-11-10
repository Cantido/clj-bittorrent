(ns clj-bittorrent.pieces.pieces
  (:require [clj-bittorrent.math.hash :as hash]
            [clj-bittorrent.math.numbers :as n]
            [clj-bittorrent.math.binary :as bin]
            [clojure.set :as s]))

(defn valid-piece? [m p]
  (let [actual-sha (hash/sha1 (:contents p))
        expected-sha (get (:pieces m) (:index p))]
    (= expected-sha actual-sha)))

(defn has-index? [n m]
  (= n (:index m)))

(defn select-index [n xrel]
  {:pre [(some? n) (not (neg? n))]}
  (s/select (partial has-index? n) (set xrel)))

(defn remove-indexed [n xrel]
  {:pre [(some? n) (not (neg? n))]}
  (s/difference (set xrel) (select-index n (set xrel))))
