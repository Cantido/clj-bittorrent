(ns clj-bittorrent.message.encode-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.message.encode :as msg]
            [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(def expected-pstrlen [0x13])

(def expected-pstr
  [0x42 0x69 0x74 0x54
   0x6F 0x72 0x72 0x65
   0x6E 0x74 0x20 0x70
   0x72 0x6F 0x74 0x6F
   0x63 0x6F 0x6C])

(def expected-reserved
  (take 8 (repeat 0x00)))

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

(deftest handshake-test
  (is (= (concat expected-pstrlen
                 expected-pstr
                 expected-reserved
                 info-hash
                 peer-id)
         (msg/handshake {:info-hash info-hash
                         :peer-id peer-id}))))

(deftest message-method-test
  (is (= 4 (count (msg/keep-alive))))
  (is (= 5 (count (msg/choke))))
  (is (= 5 (count (msg/unchoke))))
  (is (= 5 (count (msg/interested))))
  (is (= 5 (count (msg/not-interested))))
  (is (= 9 (count (msg/have 1))))
  (is (= 6 (count (msg/bitfield [1]))))
  (is (= 17 (count (msg/request 1 1 1))))
  (is (= 14 (count (msg/piece 1 1 [1]))))
  (is (= 17 (count (msg/cancel 1 1 1))))
  (is (= 7 (count (msg/port 1)))))
