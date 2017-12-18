(ns clj-bittorrent.message.handshake-test
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [clj-bittorrent.message.handshake :as handshake]))

(use-fixtures :once st/validate-schemas)

(def expected-pstrlen [0x13])

(def expected-pstr
  [0x42 0x69 0x74 0x54
   0x6F 0x72 0x72 0x65
   0x6E 0x74 0x20 0x70
   0x72 0x6F 0x74 0x6F
   0x63 0x6F 0x6C])

(def info-hash
  [0x12 0x34 0x56 0x78 0x9a
   0xbc 0xde 0xf1 0x23 0x45
   0x67 0x89 0xab 0xcd 0xef
   0x12 0x34 0x56 0x78 0x9a])

(def peer-id
  [0x67 0x89 0xab 0xcd 0xef
   0x12 0x34 0x56 0x78 0x9a
   0x12 0x34 0x56 0x78 0x9a
   0xbc 0xde 0xf1 0x23 0x45])

(def expected-reserved
  (take 8 (repeat 0x00)))

(deftest handshake-test
  (is (= (concat expected-pstrlen
                 expected-pstr
                 expected-reserved
                 info-hash
                 peer-id)
         (handshake/encode
           {:info-hash info-hash
            :peer-id peer-id}))))
