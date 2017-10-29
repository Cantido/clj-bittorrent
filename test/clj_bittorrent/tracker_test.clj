(ns clj-bittorrent.tracker-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.tracker :as tracker]
            [clojure.java.io :as io]
            [clj-bittorrent.metainfo :as m]
            [clj-bittorrent.binary :as b])
  (:import (java.io File)
           (java.security MessageDigest)))

(def info-hash
  [0x12 0x34 0x56 0x78 0x9a
   0xbc 0xde 0xf1 0x23 0x45
   0x67 0x89 0xab 0xcd 0xef
   0x12 0x34 0x56 0x78 0x9a])

(def expected-urlencoded-info-hash
  (str "%124Vx%9A"
       "%BC%DE%F1%23E"
       "g%89%AB%CD%EF"
       "%124Vx%9A"))

(deftest decode-peer-binary-entry-test
  (let [result (#'tracker/decode-peer-binary-entry
                 [48 -78 7 -102 32 1])]
    (is (= "48.178.7.154" (:ip result)))
    (is (= 8193 (:port result)))))

(deftest decode-peers-binary-test
  (let [result (#'tracker/decode-peers-binary
                 [48 -78 7 -102 32 1])]
    (is (= [{:ip "48.178.7.154" :port 8193}] result))
    (is (= {:ip "48.178.7.154" :port 8193} (first result)))))

(def example-request
  {:info-hash info-hash
   :peer-id info-hash
   :port 6881
   :uploaded 1000
   :downloaded 1000
   :left 1000
   :compact 1
   :event "started"
   :ip "192.168.1.1"
   :numwant 20})

(def example-response
  {:request-time 39
   :repeatable? false
   :protocol-version {:name "HTTP", :major 1, :minor 1}
   :streaming? true
   :chunked? false
   :reason-phrase "OK"
   :headers {"Content-Type" "text/plain", "Content-Length" 98}
   :orig-content-encoding nil
   :status 200
   :length 98
   :body "d8:completei0e10:downloadedi0e10:incompletei1e8:intervali1850e12:min intervali925e5:peers6:� �e",
   :trace-redirects []})


(deftest tracker-request-test
  (let [request (#'tracker/tracker-request
                  "http://localhost:6969/announce"
                  example-request)]
    (is (= :get (:method request)))
    (is (= (format
             "http://localhost:6969/announce?info_hash=%s&peer_id=%s"
             expected-urlencoded-info-hash
             expected-urlencoded-info-hash)
           (:url request)))
    (let [params (:query-params request)]
      (is (= nil (:peer-id params)))
      (is (= nil (:info-hash params)))
      (is (= 6881 (:port params)))
      (is (= 1000 (:uploaded params)))
      (is (= 1000 (:downloaded params)))
      (is (= 1000 (:left params)))
      (is (= 1 (:compact params)))
      (is (= "started" (:event params)))
      (is (= "192.168.1.1" (:ip params)))
      (is (= 20 (:numwant params))))))

(deftest tracker-response-test
  (let [result (#'tracker/tracker-response example-response)]
    (is (not= true (nil? result)))
    (is (= 0 (get result "complete")))
    (is (= 0 (get result "downloaded")))
    (is (= 1 (get result "incomplete")))
    (is (pos? (get result "interval")))
    (is (pos? (get result "min interval")))
    (is (= (list {:ip "239.191.189.17" :port 8193})
           (get result "peers")))))
