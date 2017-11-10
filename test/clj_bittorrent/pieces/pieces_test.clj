(ns clj-bittorrent.pieces.pieces-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.pieces.pieces :as pieces]))

(deftest has-index-test
  (is (= true
         (pieces/has-index? 5
          {:index 5})))
  (is (= false
         (pieces/has-index? 1
          {:index 5}))))

(deftest select-index-test
  (is (= #{{:index 0}}
         (pieces/select-index 0
           #{{:index 0}
             {:index 1}})))
  (is (= #{}
         (pieces/select-index 0
           #{{:index 1}
             {:index 2}}))))

(deftest remove-indexed-test
  (is (= #{}
         (pieces/remove-indexed 0
           #{{:index 0}})))
  (is (= #{{:index 1}}
         (pieces/remove-indexed 0
           #{{:index 0} {:index 1}})))
  (is (= #{{:index 0}}
         (pieces/remove-indexed 1
           #{{:index 0}}))))

