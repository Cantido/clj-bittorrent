(ns clj-bittorrent.pieces.blocks
  "Assemble blocks of data into larger blocks."
  (:require [clojure.set :as s]
            [schema.core :as schema]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.math.numbers :as n]))


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

(defn- remove-matching
  "Removes elements from xrel with values matching the map m."
  [xrel m]
  (s/difference (set xrel) (get (s/index xrel (keys m))
                                m)))


(defn remove-blocks-matching-indices
  ([bs n]
   {:pre [(n/nonneg? n)]}
   (remove-matching bs {:index n}))
  ([bs n & more]
   (reduce remove-blocks-matching-indices
           (remove-blocks-matching-indices bs n)
           more)))

(defn conj-condense
  "Adds x to the set s, condensing it into another block if possible."
  [s x]
  (let [[adj1 adj2] (filter (partial adjacent-blocks? x) s)
        superblock (concat-blocks adj1 x adj2)
        shrunken-s (disj (set s) adj1 adj2)]
    (conj (set shrunken-s) superblock)))
