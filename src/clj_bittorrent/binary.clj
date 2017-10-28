(ns clj-bittorrent.binary
  "Byte-manipulation functions.")

(defn ubyte? [x] (<= 0 x 255))
(defn sbyte? [x] (<= -128 x 127))

(defn ubyte
  "converts a signed byte to an unsigned byte.
   Unsigned bytes must be stored as integers."
  [b]
  {:pre [(sbyte? b)]
   :post [(ubyte? %)]}
  (Byte/toUnsignedInt b))

(defn sbyte
  "Converts an unsigned byte into a signed byte."
  [b]
  {:pre [(ubyte? b)]
   :post [(sbyte? %)]}
  (unchecked-byte (+ 256 b)))

(defn rand-ubyte []
  {:post [(ubyte? %)]}
  (rand-int 256))

(defn rand-sbyte []
  {:post [(sbyte? %)]}
  (sbyte (rand-ubyte)))

(defn hexbyte [b]
  {:pre [(ubyte? b)]
   :post [(= 2 (count (seq %)))]}
  (format "%02X" b))
