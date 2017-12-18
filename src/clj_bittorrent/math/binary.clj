(ns clj-bittorrent.math.binary
  "Byte-manipulation functions."
  (:require [clojure.string :as string]
            [schema.core :as schema]
            [clj-bittorrent.math.numbers :as n]))

(schema/defn max-at-bits :- schema/Int
  "Find the maximum value that n bits can represent."
  [n :- schema/Int]
  (bigint (Math/pow 2 n)))

(schema/defn max-at-bytes :- schema/Int
  "Find the maximum value that n bytes can represent."
  [n :- schema/Int]
  (max-at-bits (* n 8)))

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

(defn all-signed?
  "Test if x contains only signed bytes."
  [x]
  (every? sbyte? x))

(def SignedByte
  "An eight-bit signed byte in the range of [-128, 127]"
  (schema/constrained schema/Int sbyte? "signed-byte?"))

(def UnsignedByte
  "An eight-bit unsigned byte in the range of [0, 255]"
  (schema/constrained schema/Int ubyte? "unsigned-byte?"))

(def ByteArray
  "An ordered collection of signed bytes"
  (schema/pred all-signed?))

(def HexFormattedByte
  (schema/constrained schema/Str #(re-matches #"[0-9A-F]{2}" %)))

(schema/defn bitfield-byte :- #{(schema/constrained schema/Int #(<= 0 % 7))}
  "Returns the set of indices (left-right) of the byte that are
   set to 1."
  [x :- SignedByte]
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

(schema/defn bitfield-set :- #{schema/Int}
  "Returns the set of indices of all 1-bits in the given byte array.
   Zero indexed and capped at the total number of bits minus one"
  [x :- ByteArray]
  {:post [(<= (count %) (* 8 (count x)))]}
  (reduce-kv reduce-bitfield-array #{} (vec x)))

(schema/defn ubyte :- UnsignedByte
  "converts a signed byte to an unsigned byte.
   Unsigned bytes must be stored as integers."
  [b :- SignedByte]
  (Byte/toUnsignedInt (byte b)))

(schema/defn sbyte :- SignedByte
  "Converts an unsigned byte into a signed byte."
  [b :- UnsignedByte]
  (unchecked-byte (+ 256 b)))

(schema/defn int-from-bytes :- schema/Int
  "Creates an bigint from a seq of bytes"
  [xs :- ByteArray]
  (BigInteger. (byte-array (seq xs))))

(schema/defn hexbyte :- HexFormattedByte
  "Formats an unsigned byte into a two-character hexidecimal code."
  [b :- UnsignedByte]
  (format "%02X" (int b)))

(schema/defn ipv4-address :- schema/Str
  "Decodes a seq of four unsigned bytes into an IPv4 address."
  [s :- [UnsignedByte]]
  {:pre [(= 4 (count (seq s)))
         (every? ubyte? (seq s))]
   :post [(some? %)
          (<= 7 (count (seq %)))
          (>= 15 (count (seq %)))
          (= 3 (count (filter #{\.} %)))
          (<= 4 (count (remove #{\.} %)))
          (>= 12 (count (remove #{\.} %)))]}
  (string/join "." (seq s)))

(schema/defn pad-bytes :- ByteArray
  "Left-pads a byte array to the given size.
   Returns the array if it's equal to or bigger than n.
   This will not shrink the array."
  [n x]
  (if (< (count x) n)
    (recur n (cons 0x00 x))
    x))

(schema/defn int-bytearray :- ByteArray
  "Converts an integer into a byte array."
  [x :- n/NonNegativeInt]
  {:pre [(not (neg? x))]
   :post [(<= 1 (count %))]}
  (.toByteArray (BigInteger/valueOf (int x))))

(schema/defn int-byte-field :- ByteArray
  "Converts an integer x into a byte array of size n."
  [n :- n/Length
   x :- n/NonNegativeInt]
  {:pre [(< x (Math/pow 2 (* n 8)))]
   :post [(<= n (count %))]}
  (pad-bytes n (int-bytearray x)))

(defn sbytes-to-char-array
  [bs]
  (char-array
    (map
      (comp ubyte)
      bs)))
