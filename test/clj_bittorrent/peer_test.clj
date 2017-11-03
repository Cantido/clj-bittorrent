(ns clj-bittorrent.peer-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.peer :as peer]))

(deftest add-piece-test
  (is (= {:pieces #{666} :requested #{{:index 420}}}
         (peer/add-piece {:requested #{{:index 420} {:index 666}}}
                         666))))
