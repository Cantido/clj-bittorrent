(ns clj-bittorrent.urlencode-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.urlencode :as u]
            [clojure.test.check.clojure-test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clj-bittorrent.binary :as b]))


(def info-hash
  [0x12 0x34 0x56 0x78 0x9a 0xbc 0xde 0xf1 0x23 0x45 0x67 0x89 0xab 0xcd 0xef 0x12 0x34 0x56 0x78 0x9a])

(def expected-urlencoded-info-hash
  "%124Vx%9A%BC%DE%F1%23Eg%89%AB%CD%EF%124Vx%9A")

(deftest urlencode-test
  (is (= "%12" (u/urlencode [0x12])))
  (is (= "4" (u/urlencode [0x34])))
  (is (= "V" (u/urlencode [0x56])))
  (is (= "x" (u/urlencode [0x78])))
  (is (= "%9A" (u/urlencode [0x9a])))
  (is (= "%00" (u/urlencode [0x00])))
  (is (= expected-urlencoded-info-hash (u/urlencode info-hash))))

(declare encoded-spec x)

(defspec encoded-spec
  (prop/for-all [x gen/byte]
    (let [encoded (u/urlencode [(b/ubyte x)])
          enc-length (count encoded)]
      (or (and (= 1 enc-length)
               (Character/isDefined (char (first (seq encoded)))))
          (and (= 3 enc-length)
               (clojure.string/starts-with? encoded "%"))))))

