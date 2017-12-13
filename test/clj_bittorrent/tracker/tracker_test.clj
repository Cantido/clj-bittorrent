(ns clj-bittorrent.tracker.tracker-test
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [schema.core :as schema]
            [clojure.java.io :as io]
            [clj-bittorrent.math.binary :as bin]
            [clj-bittorrent.metainfo.metainfo :as m]
            [clj-bittorrent.metainfo.schema :as mschema]
            [clj-bittorrent.peer.peer :as peer]
            [clj-bittorrent.tracker.tracker :as tracker])
  (:import (java.io File)
           (java.security MessageDigest)))

(use-fixtures :once st/validate-schemas)

(schema/def info-hash :- mschema/InfoHash
  [18 52 86 120 -102 -68 -34 -15 35 69 103 -119 -85 -51 -17 18 52 86 120 -102])

(schema/def peer-id :- peer/PeerId
  [18 52 86 120 -102 -68 -34 -15 35 69 103 -119 -85 -51 -17 18 52 86 120 -102])

(def expected-urlencoded-info-hash
  (str "%124Vx%9A"
       "%BC%DE%F1%23E"
       "g%89%AB%CD%EF"
       "%124Vx%9A"))

(deftest decode-peer-binary-entry-test
  (let [result (#'tracker/decode-peer-binary-entry
                 [48 -78 7 -102 32 1])]
    (is (= "48.178.7.154" (:ip result)))
    (is (= 8193 (:port result))))
  (let [result (#'tracker/decode-peer-binary-entry
                 (map bin/sbyte [192 168 1 100 26 225]))]
    (is (= "192.168.1.100" (:ip result)))
    (is (= 6881 (:port result)))))

(deftest decode-peers-binary-test
  (let [result (#'tracker/decode-peers-binary
                 [48 -78 7 -102 32 1])]
    (is (= [{:ip "48.178.7.154" :port 8193}] result))
    (is (= {:ip "48.178.7.154" :port 8193} (first result)))))

(schema/def example-request :- tracker/TrackerRequest
  {:info-hash info-hash
   :peer-id peer-id
   :port 6881
   :uploaded 1000
   :downloaded 1000
   :left 1000
   :compact 1
   :event "started"
   :ip "192.168.1.1"
   :numwant 20})

(def example-response-body
  (concat
    "d"
    "8:complete" "i0e"
    "10:incomplete" "i1e"
    "8:interval" "i1850e"
    "12:min interval" "i925e"
    "5:peers" "6:" (map bin/sbyte [192 168 1 100 26 225])
    "e"))

(def example-response
  {:request-time          39
   :repeatable?           false
   :protocol-version      {:name "HTTP", :major 1, :minor 1}
   :streaming?            true
   :chunked?              false
   :reason-phrase         "OK"
   :headers               {"Content-Type" "text/plain", "Content-Length" 98}
   :orig-content-encoding nil
   :status                200
   :length                98
   :body                  (byte-array (map int example-response-body))
   :trace-redirects       []})


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

;; Uncomment & use this test if you want to hit a
;; real tracker server running locally.
;(deftest integration-test
;  (is (= {} (tracker/announce
;              "http://localhost:6969/announce"
;              example-request))))

(deftest schema-test
  (is (= nil (schema/check tracker/Interval 1000)))
  (is (= nil (schema/check tracker/CompleteCount 1000)))
  (is (= nil (schema/check tracker/IncompleteCount 1000)))
  (is (= nil (schema/check tracker/Event "started")))
  (is (= nil (schema/check tracker/Event "stopped")))
  (is (= nil (schema/check tracker/Event "completed"))))

(deftest tracker-response-test
  (let [result (#'tracker/tracker-response example-response)]
    (is (not= true (nil? result)))
    (is (= 0 (get result :complete)))
    (is (= 1 (get result :incomplete)))
    (is (= 1850 (get result :interval)))
    (is (= (list {:ip "192.168.1.100" :port 6881})
           (get result :peers)))))
