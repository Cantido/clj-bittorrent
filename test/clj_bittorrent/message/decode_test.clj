(ns clj-bittorrent.message.decode-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.message.decode :as msg]
            [schema.test :as st]))

(use-fixtures :once st/validate-schemas)

(deftest recv-message-test
  (is (= {:id :keep-alive}
         (msg/recv
           [0x00 0x00 0x00 0x00])))

  (is (= {:id :choke}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x01]
             [0x00]))))

  (is (= {:id :unchoke}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x01]
             [0x01]))))

  (is (= {:id :interested}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x01]
             [0x02]))))

  (is (= {:id :not-interested}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x01]
             [0x03]))))

  (is (= {:id :have
          :index 6969}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x03]
             [0x04]
             [0x1b 0x39]))))

  (is (= {:id :bitfield
          :indices #{1}}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x02]
             [0x05]
             [0x40]))))

  (is (= {:id :request
          :index 1
          :offset 2
          :length 3}
         (msg/recv
           (concat
             [0x00 0x00 0x00 13]
             [0x06]
             [0x00 0x00 0x00 0x01]
             [0x00 0x00 0x00 0x02]
             [0x00 0x00 0x00 0x03]))))
  (is (= {:id :piece
          :index 7
          :offset 12
          :contents (seq [0x12 0x34 0x56 0x78 0x9a])}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x0D]
             [0x07]
             [0x00 0x00 0x00 0x07]
             [0x00 0x00 0x00 0x0c]
             [0x12 0x34 0x56 0x78 0x9a]))))
  (is (= {:id :cancel
          :index 1
          :offset 2
          :length 3}
         (msg/recv
           (concat
             [0x00 0x00 0x00 13]
             [0x08]
             [0x00 0x00 0x00 0x01]
             [0x00 0x00 0x00 0x02]
             [0x00 0x00 0x00 0x03]))))
  (is (= {:id :port
          :port 6969}
         (msg/recv
           (concat
             [0x00 0x00 0x00 0x03]
             [0x09]
             [0x1b 0x39])))))
