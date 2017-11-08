(ns clj-bittorrent.urlencode
  "Make url-encoded byte arrays."
  (:require [clj-bittorrent.binary :as b]
            [clj-bittorrent.binary :as bin]
            [schema.core :as schema]))


(defn- char-range-integers [a b]
  {:pre [(< (int (char a)) (int (char b)))]}
  (set (range (int (char a))
              (inc (int (char b))))))

(def ^:private digits (char-range-integers \0 \9))

(def ^:private alpha
  (clojure.set/union
    (set (range (int \A) (inc (int \Z))))
    (set (range (int \a) (inc (int \z))))))

(def ^:private punct
  (set (map int #{\. \- \_ \~})))

(defn- allowed-raw? [b]
  (or
    (digits (int b))
    (alpha (int b))
    (punct (int b))))

(def UrlEncodedByte
  (schema/constrained [Character] #(#{1 3} (count (seq %)))))

(schema/defn urlencode-byte :- UrlEncodedByte
  [b :- bin/UnsignedByte]
  (let [ib (int b)
        result
           (if (allowed-raw? ib)
             (list (char b))
             (seq (str "%" (b/hexbyte (int b)))))]
    result))

(defn urlencode
  "Url-encodes a seq of bytes. Makes no assumptions about encoding of strings."
  [s]
  (->>
    s
    (seq)
    (map bin/ubyte)
    (mapcat urlencode-byte)
    (apply str)))
