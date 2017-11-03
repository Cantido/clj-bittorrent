(ns clj-bittorrent.peer-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.peer :as peer]))

(deftest choke-test
  (is (= true (:choked (peer/choke {:choked false}))))
  (is (= true (:choked (peer/choke {:choked true}))))
  (is (= true (:choked (peer/choke {})))))

(deftest unchoke-test
  (is (= false (:choked (peer/unchoke {:choked false}))))
  (is (= false (:choked (peer/unchoke {:choked true}))))
  (is (= false (:choked (peer/unchoke {})))))

(deftest interested-test
  (is (= true (:interested (peer/interested {:interested false}))))
  (is (= true (:interested (peer/interested {:interested true}))))
  (is (= true (:interested (peer/interested {})))))

(deftest not-interested-test
  (is (= false (:interested (peer/not-interested {:interested false}))))
  (is (= false (:interested (peer/not-interested {:interested true}))))
  (is (= false (:interested (peer/not-interested {})))))

(deftest add-piece-test
  (is (= #{666} (:pieces (peer/add-piece {:pieces #{}} 666))))
  (let [result (peer/add-piece {:requested #{{:index 420} {:index 666}}} 666)]
    (is (= #{666} (:pieces result)))
    (is (= #{{:index 420}} (:requested result))))
  (let [result (peer/add-piece {:requested #{{:index 420} {:index 666}}} 666 420)]
    (is (= #{666 420} (:pieces result)))
    (is (= #{} (:requested result)))))

(deftest request-test
  (is (thrown? AssertionError (:requested (peer/request {} {}))))
  (is (thrown? AssertionError (:requested (peer/request {} {:index -1 :offset 222 :length 333}))))
  (is (thrown? AssertionError (:requested (peer/request {} {:index 111 :offset -1 :length 333}))))
  (is (thrown? AssertionError (:requested (peer/request {} {:index 111 :offset 222 :length 0}))))
  (is (= #{{:index 0 :offset 0 :length 1}} (:requested (peer/request {} {:index 0 :offset 0 :length 1}))))
  (is (= #{{:index 111 :offset 222 :length 333}} (:requested (peer/request {} {:index 111 :offset 222 :length 333})))))
