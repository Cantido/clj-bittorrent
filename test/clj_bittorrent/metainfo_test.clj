(ns clj-bittorrent.metainfo-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-bittorrent.metainfo :as m])
  (:import (java.io File)))

(def metainfo-file ^File
  (io/file (io/resource "linuxmint-18.2-cinnamon-64bit.iso.torrent")))

(def iso-file ^File
  (io/file (io/resource "linuxmint-18.2-cinnamon-64bit.iso")))

(defn hex [n]
  (format "%02x" n));

(deftest read-file
  (let [result (m/read-metainfo metainfo-file)]
    (is (= 1599 (m/expected-piece-count result)))
    (is (= "https://torrents.linuxmint.com/announce.php" (:announce result)))
    (is (= "Transmission/2.84 (14307)" (:created-by result)))
    (is (= 1499021259 (:creation-date result)))
    (is (= "UTF-8" (:encoding result)))
    (let [info (:info result)]
      (is (not= nil info))
      (is (= 1676083200 (:length info)))
      (is (= "linuxmint-18.2-cinnamon-64bit.iso" (:name info)))
      (is (= 1048576 (:piece-length info)))
      (is (= 0 (:private info)))
      ;(is (= true (every? #(= 20 (count %)) (:pieces info))))
      ;(is (= "ef" (hex (ffirst (:pieces info)))))
      (is (= (* 20 1599) (count (partition 20 (:pieces info))))))))

