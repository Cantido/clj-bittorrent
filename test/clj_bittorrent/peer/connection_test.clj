(ns clj-bittorrent.peer.connection-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.peer.connection :as conn]))

(deftest download?-test
  (is (= true (conn/download-allowed? {:client {:interested true}
                                       :peer   {:choked false}})))
  (is (= false (conn/download-allowed? {:client {:interested true}
                                        :peer   {:choked true}})))
  (is (= false (conn/download-allowed? {:client {:interested false}
                                        :peer   {:choked true}})))
  (is (= false (conn/download-allowed? {:client {:interested false}
                                        :peer   {:choked false}}))))

(deftest upload?-test
  (is (= true (conn/upload-allowed? {:client {:choked false}
                                     :peer   {:interested true}})))
  (is (= false (conn/upload-allowed? {:client {:choked true}
                                      :peer   {:interested true}})))
  (is (= false (conn/upload-allowed? {:client {:choked false}
                                      :peer   {:interested false}})))
  (is (= false (conn/upload-allowed? {:client {:choked true}
                                      :peer   {:interested false}}))))
