(ns clj-bittorrent.binary-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.binary :as bin]
            [clojure.test.check.clojure-test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(deftest ubyte-test
  (is (= 239 (bin/ubyte -17))))

(deftest sbyte-test
  (is (= -17 (bin/sbyte 239))))

(deftest ipv4-address-test
  (is (= "192.168.1.100" (bin/ipv4-address [192 168 1 100])))
  (is (= "127.0.0.1" (bin/ipv4-address [127 0 0 1])))
  (is (= "0.0.0.0" (bin/ipv4-address [0 0 0 0])))
  (is (= "255.255.255.255" (bin/ipv4-address [255 255 255 255]))))

(declare reflective-spec sbyte-spec x)

(defspec reflective-spec
  (prop/for-all [x gen/byte]
    (is (= x (-> x bin/ubyte bin/sbyte)))))

(defspec sbyte-spec
  (prop/for-all [x gen/byte]
    (is (bin/sbyte? x))
    (is (bin/ubyte? (bin/ubyte x)))))
