(ns clj-bittorrent.urlencode
  "Make url-encoded byte arrays."
  (:require [clj-bittorrent.binary :as b]
            [clj-bittorrent.binary :as bin]))


(defn- char-range-integers [a b]
  {:pre [(< (int (char a)) (int (char b)))]}
  (set (range (int (char a))
              (+ 1 (int (char b))))))

(def digits (char-range-integers \0 \9))

(def alpha (clojure.set/union
             (set (range (int \A) (+ 1 (int \Z))))
             (set (range (int \a) (+ 1 (int \z))))))

(def punct
  (set (map int #{\. \- \_ \~})))

(defn- allowed-raw? [b]
  (or
    (digits (int b))
    (alpha (int b))
    (punct (int b))))


(defn- urlencode-byte [b]
  {:pre [(b/ubyte? b)]
   :post [(#{1 3} (count (seq %)))
          (seq? %)]}
  (let [ib (int b)
        result
           (if (allowed-raw? ib)
             (list (char b))
             (seq (str "%" (b/hexbyte (int b)))))]
    result))

(defn urlencode
  "Url-encodes a seq of bytes. The bytes are assumed to be unsigned.
   Makes no assumptions about encoding of strings."
  [s]
  (->>
    s
    (seq)
    (map int)
    (mapcat urlencode-byte)
    (apply str)))

