(ns clj-bittorrent.metainfo-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-bittorrent.metainfo :as m]
            [clj-bittorrent.binary :as b])
  (:import (java.io File)))

(def metainfo-file ^File
  (io/file (io/resource "linuxmint-18.2-cinnamon-64bit.iso.torrent")))

(def metainfo-multi-file ^File
  (io/file (io/resource "multifile.torrent")))

(defn mapreduce [mfn rfn coll]
  (reduce rfn (map mfn (seq coll))))

(deftest read-file-test
  (let [result (m/read metainfo-file)]
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
      (is (= 1599 (count (:pieces info))))
      (is (= 31980 (mapreduce count + (:pieces info))))
      (is (= true (every? b/ubyte? (apply concat (:pieces info))))))))

(deftest read-multi-file-torrent-test
  (let [result (m/read metainfo-multi-file)]
    (is (= "http://bt1.archive.org:6969/announce" (:announce result)))
    (is (= [["http://bt1.archive.org:6969/announce"]
            ["http://bt2.archive.org:6969/announce"]]
           (:announce-list result)))
    (is (= "ia_make_torrent" (:created-by result)))
    (is (= 1507657955 (:creation-date result)))
    (let [info (:info result)]
      (is (= "AboutBan1935" (:name info)))
      (is (= 524288 (:piece-length info)))
      (is (= 33880 (mapreduce count + (:pieces info))))
      (is (= 1694 (count (:pieces info))))
      (is (= nil (:private info)))
      (is (not= nil info))
      (let [files (:files info)]
        (is (= 15 (count files)))
        (let [first-file (first files)]
          (is (= 4279 (:length first-file)))
          (is (= "aae98423363bcafc54b03289f222612b9e0004b0" (get first-file "sha1")))
          (is (= "54cf30db294b07c6a26293c0a8aa8aee" (get first-file "md5")))
          (is (= "44504df6" (get first-file "crc32"))))))))
