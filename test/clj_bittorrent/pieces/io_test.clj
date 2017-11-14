(ns clj-bittorrent.pieces.io-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-bittorrent.pieces.io :as pio])
  (:import (java.io Reader)))

(def test-file (io/resource "testfile.bin"))

(use-fixtures :each
              (fn [f]
                (spit test-file "")
                (f)
                (spit test-file "")))

(def abc-meta {:piece-length 3 :pieces [1]})

(def abc-piece {:index 0 :contents [97 98 99]})

(deftest write-pieces-test
  (pio/write-pieces abc-meta #{abc-piece} test-file)
  (is (= "abc" (slurp test-file))))
