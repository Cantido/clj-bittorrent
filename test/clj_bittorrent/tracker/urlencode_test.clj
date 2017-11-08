(ns clj-bittorrent.tracker.urlencode-test
  (:require [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clj-bittorrent.math.binary :as b]
            [clj-bittorrent.tracker.urlencode :as u]))


(def info-hash
  [18 52 86 120 -102 -68 -34 -15 35 69 103 -119 -85 -51 -17 18 52 86 120 -102])

(def expected-urlencoded-info-hash
  (str "%124Vx%9A"
       "%BC%DE%F1%23E"
       "g%89%AB%CD%EF"
       "%124Vx%9A"))

(deftest urlencode-test
  (is (= "%12" (u/urlencode [18])))
  (is (= "4" (u/urlencode [52])))
  (is (= "V" (u/urlencode [86])))
  (is (= "x" (u/urlencode [120])))
  (is (= "%9A" (u/urlencode [-102])))
  (is (= "%00" (u/urlencode [0])))
  (is (= expected-urlencoded-info-hash (u/urlencode info-hash))))

(declare encoded-spec x)

(defspec encoded-spec
  (prop/for-all [x gen/byte]
    (let [encoded (u/urlencode [x])
          enc-length (count encoded)]
      (or (and (= 1 enc-length)
               (Character/isDefined (char (first (seq encoded)))))
          (and (= 3 enc-length)
               (clojure.string/starts-with? encoded "%"))))))
