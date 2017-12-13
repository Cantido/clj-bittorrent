(ns clj-bittorrent.pieces.pieces
  (:require [clojure.set :as s]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.hash :as hash]
            [clj-bittorrent.math.numbers :as n]))

(defn valid-piece? [m p]
  (let [actual-sha (seq (hash/sha1 (byte-array (:contents p))))
        expected-sha (seq (byte-array (get (:pieces m) (:index p))))]
    (= expected-sha actual-sha)))

(defn valid-pieces [m p]
  (s/select
    (partial valid-piece? m)
    (set p)))

(defn invalid-pieces [m p]
  (s/difference
    (set p)
    (valid-pieces m p)))

(defn validate [m p]
  (let [valid (valid-pieces m p)
        invalid (s/difference (set p) valid)]
    [valid invalid]))

(defn has-index? [n m]
  (= n (:index m)))

(defn select-index [n xrel]
  {:pre [(some? n) (not (neg? n))]}
  (s/select (partial has-index? n) (set xrel)))

(defn remove-indexed [n xrel]
  {:pre [(some? n) (not (neg? n))]}
  (s/difference (set xrel) (select-index n (set xrel))))
