(ns clj-bittorrent.peer
  "Manipulate peer maps."
  (:require [clj-bittorrent.binary :as bin]
            [clojure.set :as s])
  (:import (java.nio.charset StandardCharsets)))

(def peer-default-state
  {:choked true
   :interested false
   :have #{}
   :blocks #{}
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

(defn- keys=
  "Compare a's and b's value for key k."
  ([k x] true)
  ([k x y] (= (k x) (k y)))
  ([k x y & more]
   (if (keys= k x y)
     (if (next more)
       (recur k y (first more) (next more))
       (keys= k y (first more)))
     false)))

(defn- sequential-blocks?
  "Returns true if a is immediately followed by b"
  [a b]
  (= (+ (:offset a) (count (:contents a)))
     (:offset b)))

(defn- adjacent-blocks? [a b]
  (or (sequential-blocks? a b)
      (sequential-blocks? b a)))

(defn- concat-blocks
  "Join two blocks into one larger block"
  ([a b]
   {:post [(= (count (:contents %))
              (+ (count (:contents a))
                 (count (:contents b))))]}
   (cond
     (nil? a) b
     (nil? b) a
     :else
     (do
       (assert (keys= :index a b))
       (assert (not (keys= :offset a b)))
       (assert (adjacent-blocks? a b))
       (if (< (:offset a) (:offset b))
         (do
           (assert (sequential-blocks? a b))
           (update a :contents #(concat % (:contents b))))
         (do
           (assert (sequential-blocks? b a))
           (update b :contents #(concat % (:contents a))))))))
  ([a b & more]
   (reduce concat-blocks (concat-blocks a b) more)))

(defn- conj-condense
  "Adds x to the set s, condensing it into another block if possible."
  [s x]
  (let [[adj1 adj2] (filter (partial adjacent-blocks? x) s)
        superblock (concat-blocks adj1 x adj2)
        shrunken-s (disj s adj1 adj2)]
    (conj shrunken-s superblock)))

(defn- remove-blocks-matching-indices
  ([b n] (remove #(= n (:index %)) b))
  ([b n & ns]
   (reduce remove-blocks-matching-indices
           (remove-blocks-matching-indices b n)
           ns)))

(defn has-piece [peer & ns]
  {:pre [(every? number? ns)]
   :post [(s/subset? ns (:have %))]}
  (-> peer
    (update :have #(s/union % (set ns)))
    (update :requested #(set (apply remove-blocks-matching-indices % ns)))))

(defn add-block [peer block]
  (-> peer
    (update :blocks #(conj-condense % (select-keys block [:index :offset :contents])))
    (update :requested #(set (remove-blocks-matching-indices % (:index block))))))

(defn request [peer block]
  {:pre [(some? block)
         (some? (:index block))
         (some? (:offset block))
         (some? (:length block))
         (not (neg? (:index block)))
         (not (neg? (:offset block)))
         (pos? (:length block))]
   :post [(not (contains? (:have %) (:index block)))
          (contains? (:requested %) block)]}
  (-> peer
    (update :have #(set (disj % (:index block))))
    (update :requested #(set (conj % block)))))
