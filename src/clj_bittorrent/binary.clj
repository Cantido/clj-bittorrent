(ns clj-bittorrent.binary
  "Byte-manipulation functions.")

(defn ubyte? ^Boolean [x] (<= 0 (int x) 255))
(defn sbyte? ^Boolean [x] (<= -128 (int x) 127))

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
  (apply str (interpose "." (seq s))))
