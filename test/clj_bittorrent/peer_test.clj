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

(deftest has-piece-test
  (is (= #{666} (:have (peer/has-piece {:have #{}} 666))))
  (let [result (peer/has-piece {:requested #{{:index 420} {:index 666}}} 666)]
    (is (= #{666} (:have result)))
    (is (= #{{:index 420}} (:requested result))))
  (let [result (peer/has-piece {:requested #{{:index 420} {:index 666}}} 666 420)]
    (is (= #{666 420} (:have result)))
    (is (= #{} (:requested result)))))

(def piece-zero {:index 0 :offset 0 :contents [0]})
(def piece-one  {:index 0 :offset 1 :contents [1]})
(def piece-two  {:index 0 :offset 2 :contents [2]})

(def piece-zero-one {:index 0 :offset 0 :contents [0 1]})
(def piece-all {:index 0 :offset 0 :contents [0 1 2]})

(deftest add-block-test
  (testing "adding a block to a peer"
    (is (= #{piece-zero-one}
           (:blocks (peer/add-block
                      {:blocks #{piece-zero}}
                      piece-one))))
    (testing "when pieces in the same block aren't adjacent"
      (is (= #{piece-zero piece-two}
             (:blocks (peer/add-block
                        {:blocks #{piece-zero}}
                        piece-two)))))
    (testing "when a third block becomes adjacent between two existing blocks"))
  (is (= #{piece-all}
         (:blocks (peer/add-block
                    {:blocks #{piece-zero piece-two}}
                    piece-one)))))

(deftest request-test
  (is (= #{{:index 0 :offset 0 :length 1}} (:requested (peer/request-block {} {:index 0 :offset 0 :length 1}))))
  (is (= #{{:index 111 :offset 222 :length 333}} (:requested (peer/request-block {} {:index 111 :offset 222 :length 333})))))
