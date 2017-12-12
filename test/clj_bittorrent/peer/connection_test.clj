(ns clj-bittorrent.peer.connection-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.peer.connection :as conn]))

(defn conn-state [client-state peer-state]
  {:client {:state client-state}
   :peer {:state peer-state}})

(defn can-download? [client-state peer-state]
  (conn/download-allowed? (conn-state client-state peer-state)))

(defn can-upload? [client-state peer-state]
  (conn/upload-allowed? (conn-state client-state peer-state)))

(deftest download?-test
  (is (= true (can-download? :interested :ready)))
  (is (= false (can-download? :interested :choked)))
  (is (= false (can-download? :ready :choked)))
  (is (= false (can-download? :ready :ready))))

(deftest upload?-test
  (is (= true (can-upload? :ready :interested)))
  (is (= false (can-upload? :choked :interested)))
  (is (= false (can-upload? :ready :ready)))
  (is (= false (can-upload? :choked :ready))))
