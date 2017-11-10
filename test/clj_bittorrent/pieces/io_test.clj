(ns clj-bittorrent.pieces.io-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-bittorrent.pieces.io :as pio])
  (:import (java.io Reader)))

(deftest write-pieces-test
  (let [test-file (io/resource "testfile.bin")]
    (is (= nil
           (pio/write-pieces
             {:piece-length 3
              :pieces [1]}
             #{{:index 0 :contents [97 98 99]}}
             test-file)))
    (spit test-file "")))

  ;(is (= "abc" (slurp (io/resource "testfile.bin")))))


