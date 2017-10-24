(ns clj-bittorrent.metainfo-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clj-bittorrent.metainfo :as m])
  (:import (java.io File)))

(def file ^File
  (io/file (io/resource "linuxmint-18.2-cinnamon-64bit.iso.torrent")))

(deftest read-file
  (let [result (m/read-metainfo file)]
    (is (= "https://torrents.linuxmint.com/announce.php" (:announce result)))))
