(ns clj-bittorrent.message-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.message :as msg]
            [clj-bittorrent.connection :as conn]))

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

(deftest recv-message-test
  (is (= {:id :keep-alive} (msg/recv [0x00 0x00 0x00 0x00])))
  (is (= {:id :choke} (msg/recv [0x00 0x00 0x00 0x00 0x00])))
  (is (= {:id :unchoke} (msg/recv [0x00 0x00 0x00 0x00 0x01])))
  (is (= {:id :interested} (msg/recv [0x00 0x00 0x00 0x00 0x02])))
  (is (= {:id :not-interested} (msg/recv [0x00 0x00 0x00 0x00 0x03])))
  (is (= {:id :have :index 6969} (msg/recv [0x00 0x00 0x00 0x00 0x04 0x1b 0x39])))
  (is (= {:id :bitfield :indices #{1}} (msg/recv [0x00 0x00 0x00 0x00 0x05 0x40])))
  (is (= {:id :request :index 1 :offset 2 :length 3}
         (msg/recv (concat
                     [0x00 0x00 0x00 13]
                     [0x06]
                     [0x00 0x00 0x00 0x01]
                     [0x00 0x00 0x00 0x02]
                     [0x00 0x00 0x00 0x03]))))
  (is (= {:id :piece :index 7 :offset 12 :contents (seq [0x12 0x34 0x56 0x78 0x9a])}
         (msg/recv (concat
                     [0x00 0x00 0x00 0x0D]
                     [0x07]
                     [0x00 0x00 0x00 0x07]
                     [0x00 0x00 0x00 0x0c]
                     [0x12 0x34 0x56 0x78 0x9a]))))
  (is (= {:id :cancel :index 1 :offset 2 :length 3}
         (msg/recv (concat
                     [0x00 0x00 0x00 13]
                     [0x08]
                     [0x00 0x00 0x00 0x01]
                     [0x00 0x00 0x00 0x02]
                     [0x00 0x00 0x00 0x03]))))
  (is (= {:id :port :port 6969}
         (msg/recv (concat
                     [0x00 0x00 0x00 0x03]
                     [0x09]
                     [0x1b 0x39])))))

(def choked-client (assoc-in conn/connection-default-state [:client :choked] true))
(def unchoked-client (assoc-in conn/connection-default-state [:client :choked] false))
(def interested-remote (assoc-in conn/connection-default-state [:peer :interested] true))
(def uninterested-remote (assoc-in conn/connection-default-state [:peer :interested] false))
(def peer-with-piece (assoc-in conn/connection-default-state [:peer :have] #{6969}))
(def peer-with-pieces (assoc-in conn/connection-default-state [:peer :have] #{6969 420 666}))
(def peer-with-requested (assoc-in conn/connection-default-state [:peer :requested] #{{:index 6969 :offset 420 :length 666}}))
(def client-has-piece (assoc-in conn/connection-default-state [:client :blocks] #{{:index 6969 :offset 420 :contents [0x23]}}))
(def peer-with-port (assoc-in conn/connection-default-state [:peer :port] 6881))

(deftest apply-msg-test
  (is (= {} (msg/apply-msg {:id :keep-alive} {})))
  (is (= choked-client (msg/apply-msg {:id :choke} unchoked-client)))
  (is (= choked-client (msg/apply-msg {:id :choke} choked-client)))
  (is (= unchoked-client (msg/apply-msg {:id :unchoke} choked-client)))
  (is (= unchoked-client (msg/apply-msg {:id :unchoke} unchoked-client)))
  (is (= interested-remote (msg/apply-msg {:id :interested} uninterested-remote)))
  (is (= interested-remote (msg/apply-msg {:id :interested} interested-remote)))
  (is (= uninterested-remote (msg/apply-msg {:id :not-interested} interested-remote)))
  (is (= uninterested-remote (msg/apply-msg {:id :not-interested} uninterested-remote)))
  (is (= peer-with-piece (msg/apply-msg {:id :have :index 6969} conn/connection-default-state)))
  (is (= peer-with-piece (msg/apply-msg {:id :have :index 6969} peer-with-piece)))
  (is (= peer-with-piece (msg/apply-msg {:id :have :index 6969} peer-with-requested)))
  (is (= peer-with-pieces (msg/apply-msg {:id :bitfield :indices #{6969 420 666}} conn/connection-default-state)))
  (is (= peer-with-pieces (msg/apply-msg {:id :bitfield :indices #{6969 420 666}} peer-with-piece)))
  (is (= peer-with-pieces (msg/apply-msg {:id :bitfield :indices #{6969 420 666}} peer-with-piece)))
  (is (= peer-with-requested (msg/apply-msg {:id :request :index 6969 :offset 420 :length 666} peer-with-piece)))
  (is (= (:client client-has-piece) (:client (msg/apply-msg {:id :piece :index 6969 :offset 420 :contents [0x23]} conn/connection-default-state))))
  (is (= (:peer conn/connection-default-state) (:peer (msg/apply-msg {:id :cancel :index 6969 :offset 420 :length 666} peer-with-requested))))
  (is (= (:peer peer-with-port) (:peer (msg/apply-msg {:id :port :port 6881} conn/connection-default-state)))))
