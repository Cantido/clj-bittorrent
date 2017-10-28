(ns clj-bittorrent.tracker-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.tracker :as tracker]
            [clojure.java.io :as io]
            [clj-bittorrent.metainfo :as m]
            [clj-bittorrent.binary :as b])
  (:import (java.io File)
           (java.security MessageDigest)))

(def metainfo-file ^File
  (io/file (io/resource "linuxmint-18.2-cinnamon-64bit.iso.torrent")))

(def metainfo
  (m/read metainfo-file))

(def peer-id
  (take 20 (repeatedly #(b/rand-ubyte))))

;; This test is designed to run against a real tracker server
;
(deftest get-initial-request-test
  (let [result (tracker/announce "http://localhost:6969/announce"
                            {:info-hash (:info-hash metainfo)
                             :peer-id peer-id
                             :port 6881
                             :uploaded 1000
                             :downloaded 1000
                             :left 1000
                             :compact 1
                             :event "started"
                             :ip "192.168.1.100"
                             :numwant 20})]
    (is (not= true (nil? result)))
    (is (= 0 (get result "complete")))
    (is (= 0 (get result "downloaded")))
    (is (= 1 (get result "incomplete")))
    (is (pos? (get result "interval")))
    (is (pos? (get result "min interval")))
    (is (= [-17 -65 -67 17 0 1] (map int (seq (get result "peers")))))))



