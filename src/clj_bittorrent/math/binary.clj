(ns clj-bittorrent.math.binary
  "Byte-manipulation functions."
  (:require [clojure.string :as string]
            [schema.core :as schema]))

(defn max-at-bits
  "Find the maximum value that n bits can represent."
  [n]
  (bigint (Math/pow 2 n)))

(defn max-at-bytes
  "Find the maximum value that n bytes can represent."
  [n]
  (max-at-bits (* n 8)))

(defn bitfield-byte
  "Returns the set of indices (left-right) of the byte that are
   set to 1."
  [x]
  (set
    (for [i (range 8)
          :when (bit-test x i)]
      (- 7 i))))

(defn- reduce-bitfield-array
  "A reduction function that fills a set with the indices in a vector
   that are set to 1."
  [init i v]
  (apply conj
         init
         (map
           (partial + (* 8 i))
           (bitfield-byte v))))

(defn bitfield-set
  "Returns the set of indices of all 1-bits in the given byte array.
   Zero indexed and capped at the total number of bits minus one"
  [x]
  {:post [(<= (count %) (* 8 (count x)))]}
  (reduce-kv reduce-bitfield-array #{} (vec x)))

(defn fits-in-bytes-signed?
  "Test if x can fit into n signed bytes."
  [n x]
  (let [exp (/ (max-at-bytes n) 2)]
    (<= (- exp)
        (bigint x)
        (dec exp))))

(defn fits-in-bytes-unsigned?
  "Test if x can fit into n unsigned bytes."
  [n x]
  (<= 0
      (bigint x)
      (dec (max-at-bytes n))))

(defn ubyte?
  "Test if x can fit into an unsigned byte."
  ^Boolean [x]
  (fits-in-bytes-unsigned? 1 x))

(defn sbyte?
  "Test if x can fit into a signed byte."
  ^Boolean [x]
  (fits-in-bytes-signed? 1 x))


(defn sint?
  "Test if x can fit into a signed four-byte integer."
  ^Boolean [x]
  (fits-in-bytes-signed? 4 x))

(def SignedByte
  "An eight-bit signed byte in the range of [-128, 127]"
  (schema/constrained schema/Int sbyte?))

(def UnsignedByte
  "An eight-bit unsigned byte in the range of [0, 255]"
  (schema/constrained schema/Int ubyte?))

(def ByteArray
  "An ordered collection of signed bytes"
  (schema/pred (partial every? sbyte?)))

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

(defn int-from-bytes
  "Creates an bigint from a seq of bytes"
  [xs]
  (BigInteger. (byte-array (seq xs))))

(defn hexbyte
  "Formats an unsigned byte into a two-character hexidecimal code."
  ^String [b]
  {:pre [(ubyte? b)]
   :post [(= 2 (count (seq %)))]}
  (format "%02X" (int b)))

(defn ipv4-address
  "Decodes a seq of four unsigned bytes into an IPv4 address."
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
