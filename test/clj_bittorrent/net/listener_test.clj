(ns clj-bittorrent.net.listener-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.net.listener :as listener]
            [clojure.java.io :as io]))

(deftest create-message-handler-test
  (let [handler (listener/create-message-handler {})
        reader (io/reader (byte-array [0x00 0x00 0x00 0x00]))
        result (handler reader nil)]
    (is (= {} result))))

(deftest build-server-test
  (listener/build-server {}))
