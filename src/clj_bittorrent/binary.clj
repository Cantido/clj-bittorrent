(ns clj-bittorrent.binary
  "Byte-manipulation functions."
  (:require [clojure.string :as string]))

(defn max-at-bits [n]
 (bigint (Math/pow 2 n)))

(defn max-at-bytes [n]
  (max-at-bits (* n 8)))

(defn fits-in-bytes-signed [n x]
  (let [exp (/ (max-at-bytes n) 2)]
    (<= (- exp)
        (bigint x)
        (- exp 1))))

(defn fits-in-bytes-unsigned [n x]
  (<= 0
      (bigint x)
      (- (max-at-bytes n) 1)))

(defn ubyte? ^Boolean [x] (fits-in-bytes-unsigned 1 x))
(defn sbyte? ^Boolean [x] (fits-in-bytes-signed 1 x))
(defn sint? ^Boolean [x] (fits-in-bytes-signed 4 x))

(defn ubyte
  "converts a signed byte to an unsigned byte.
   Unsigned bytes must be stored as integers."
  ^Integer [b]
  {:pre [(sbyte? b)]
   :post [(ubyte? %)]}
  (Byte/toUnsignedInt (byte b)))

(defn sbyte
  "Converts an unsigned byte into a signed byte."
  ^Byte [b]
  {:pre [(ubyte? b)]
   :post [(sbyte? %)]}
  (unchecked-byte (+ 256 b)))

(defn rand-ubyte
  ^Integer []
  {:post [(ubyte? %)]}
  (rand-int 256))

(defn rand-sbyte
  ^Byte []
  {:post [(sbyte? %)]}
  (sbyte (rand-ubyte)))

(defn hexbyte
  ^String [b]
  {:pre [(ubyte? b)]
   :post [(= 2 (count (seq %)))]}
  (format "%02X" (int b)))

(defn ipv4-address
  "Decodes a list of unsigned bytes into an IPv4 address."
  ^String [s]
  {:pre [(= 4 (count (seq s)))
         (every? ubyte? (seq s))]
   :post [(some? %)
          (<= 7 (count (seq %)))
          (>= 15 (count (seq %)))
          (= 3 (count (filter #{\.} %)))
          (<= 4 (count (remove #{\.} %)))
          (>= 12 (count (remove #{\.} %)))]}
  (string/join "." (seq s)))

(defn pad-bytes
  "Left-pads a byte array to the given size.
   Returns the array if it's equal to or bigger than n.
   This will not shrink the array."
  [n x]
  (if (< (count x) n)
    (recur n (cons 0x00 x))
    x))

(defn int-bytearray
  "Converts an integer into a byte array."
  [x]
  {:pre [(not (neg? x))]
   :post [(<= 1 (count %))]}
  (.toByteArray (BigInteger/valueOf (int x))))

(defn int-byte-field
  "Converts an integer x into a byte array of size n."
  [n x]
  {:pre [(< x (Math/pow 2 (* n 8)))]
   :post [(<= n (count %))]}
  (pad-bytes n (int-bytearray x)))
