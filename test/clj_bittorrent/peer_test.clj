(ns clj-bittorrent.peer-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.peer :as peer]))

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
         (peer/handshake {:info-hash info-hash
                          :peer-id peer-id}))))

(deftest download?-test
  (is (= true (peer/download? {:client {:interested true}
                               :peer {:choking false}})))
  (is (= false (peer/download? {:client {:interested true}
                                :peer {:choking true}})))
  (is (= false (peer/download? {:client {:interested false}
                                :peer {:choking true}})))
  (is (= false (peer/download? {:client {:interested false}
                                :peer {:choking false}}))))

(deftest upload?-test
  (is (= true (peer/upload? {:client {:choking false}
                             :peer {:interested true}})))
  (is (= false (peer/upload? {:client {:choking true}
                              :peer {:interested true}})))
  (is (= false (peer/upload? {:client {:choking false}
                              :peer {:interested false}})))
  (is (= false (peer/upload? {:client {:choking true}
                              :peer {:interested false}}))))
