(ns clj-bittorrent.math.binary-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clj-bittorrent.math.binary :as bin]))

(deftest sbyte?-test
  (is (= true (bin/sbyte? -128)))
  (is (= true (bin/sbyte? 127)))
  (is (= false (bin/sbyte? 128)))
  (is (= false (bin/sbyte? -129))))

(deftest ubyte?-test
  (is (= true (bin/ubyte? 0)))
  (is (= true (bin/ubyte? 1)))
  (is (= true (bin/ubyte? 255)))
  (is (= false (bin/ubyte? 256)))
  (is (= false (bin/ubyte? -1))))

(deftest ubyte-test
  (is (= 239 (bin/ubyte -17))))

(deftest sbyte-test
  (is (= -17 (bin/sbyte 239))))

(deftest bitfield-set-test
  (is (= #{} (bin/bitfield-set (byte-array [0]))))
  (is (= #{0} (bin/bitfield-set (byte-array [0x80]))))
  (is (= #{7} (bin/bitfield-set (byte-array [0x01]))))
  (is (= #{1} (bin/bitfield-set (byte-array [0x40]))))
  (is (= #{31} (bin/bitfield-set (byte-array [0x00 0x00 0x00 0x01]))))
  (is (= #{28 29 30 31} (bin/bitfield-set (byte-array [0x00 0x00 0x00 0x0F]))))
  (is (= #{1 2 3} (bin/bitfield-set (byte-array [0x70 0x00 0x00 0x00]))))
  (is (= (set (range 32)) (bin/bitfield-set (byte-array [0xFF 0xFF 0xFF 0xFF])))))


(deftest ipv4-address-test
  (is (= "192.168.1.100" (bin/ipv4-address [192 168 1 100])))
  (is (= "127.0.0.1" (bin/ipv4-address [127 0 0 1])))
  (is (= "0.0.0.0" (bin/ipv4-address [0 0 0 0])))
  (is (= "255.255.255.255" (bin/ipv4-address [255 255 255 255]))))

(deftest pad-bytes-test
  (is (= '() (bin/pad-bytes 0 [])))
  (is (= '(0) (bin/pad-bytes 0 [0x00])))
  (is (= '(0) (bin/pad-bytes 1 [0x00])))
  (is (= '(0 0) (bin/pad-bytes 2 [0x00])))
  (is (= '(0 0xDD) (bin/pad-bytes 2 [0xDD])))
  (is (= '(0xDD 0xFF) (bin/pad-bytes 1 [0xDD 0xFF]))))

(deftest int-bytearray-test
  (is (= '(0) (seq (bin/int-bytearray 0))))
  (is (= '(127) (seq (bin/int-bytearray 127))))
  (is (= '(35 41) (seq (bin/int-bytearray 9001)))))

(deftest int-byte-field-test
  (is (= '(0 0 0 1) (bin/int-byte-field 4 1)))
  (is (= '(0 0 35 41) (bin/int-byte-field 4 9001))))

(declare reflective-spec sbyte-spec x)

(defspec reflective-spec
  (prop/for-all [x gen/byte]
    (is (= x (-> x bin/ubyte bin/sbyte)))))

(defspec sbyte-spec
  (prop/for-all [x gen/byte]
    (is (bin/sbyte? x))
    (is (bin/ubyte? (bin/ubyte x)))))
