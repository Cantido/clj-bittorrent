(ns clj-bittorrent.peer.peer-test
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [clj-bittorrent.peer.peer :as peer]))

(use-fixtures :once st/validate-schemas)

(defn peer-with [key val]
  (assoc peer/peer-default-state key val))

(defn peer-state [state]
  (assoc peer/peer-default-state :state state))

(defn block-id [i]
  {:index i :offset 1 :length 1})

(deftest choke-test
  (is (= :choked (:state (peer/choke (peer-state :choked)))))
  (is (= :choked (:state (peer/choke (peer-state :ready))))))

(deftest unchoke-test
  (is (= :ready (:state (peer/unchoke (peer-state :ready)))))
  (is (= :ready (:state (peer/unchoke (peer-state :choked))))))

(deftest interested-test
  (is (= :interested (:state (peer/interested (peer-state :ready)))))
  (is (= :interested (:state (peer/interested (peer-state :interested))))))

(deftest not-interested-test
  (is (= :ready (:state (peer/not-interested (peer-state :ready)))))
  (is (= :ready (:state (peer/not-interested (peer-state :interested))))))

(deftest has-piece-test
  (is (= #{666} (:have (peer/has-piece (peer-with :have #{}) 666))))

  (let [result (peer/has-piece
                 (peer-with :requested #{(block-id 420) (block-id 666)})
                 666)]

    (is (= #{666} (:have result)))
    (is (= #{(block-id 420)} (:requested result))))

  (let [result (peer/has-piece
                 (peer-with :requested #{(block-id 420) (block-id 666)})
                 666 420)]

    (is (= #{666 420} (:have result)))
    (is (= #{} (:requested result))))
  (let [result (peer/has-piece
                 (peer-with :pending-verify #{{:index 420 :offset 0 :contents [1]}})
                 420)]
    (is (= #{} (:pending-verify result)))))

(def piece-zero {:index 0 :offset 0 :contents [0]})
(def piece-one  {:index 0 :offset 1 :contents [1]})
(def piece-two  {:index 0 :offset 2 :contents [2]})

(def piece-zero-one {:index 0 :offset 0 :contents [0 1]})
(def piece-all {:index 0 :offset 0 :contents [0 1 2]})

(deftest add-block-test
  (testing "adding a block to a peer"
    (is (= #{piece-zero-one}
           (:blocks (peer/add-block
                      (peer-with :blocks #{piece-zero})
                      piece-one))))
    (testing "when pieces in the same block aren't adjacent"
      (is (= #{piece-zero piece-two}
             (:blocks (peer/add-block
                        (peer-with :blocks #{piece-zero})
                        piece-two)))))
    (testing "when a third block becomes adjacent between two existing blocks"))
  (is (= #{piece-all}
         (:blocks (peer/add-block
                    (peer-with :blocks #{piece-zero piece-two})
                    piece-one)))))

(deftest request-test
  (is (= #{{:index 0 :offset 0 :length 1}}
         (:requested (peer/request-block
                       peer/peer-default-state
                       {:index 0 :offset 0 :length 1}))))
  (is (= #{{:index 111 :offset 222 :length 333}}
         (:requested (peer/request-block
                       peer/peer-default-state
                       {:index 111 :offset 222 :length 333})))))

(def finished-block
  {:index 0 :offset 0 :contents [0 1 2]})


(def unfinished-block
  {:index 1 :offset 0 :contents [0]})

(def finished-peer
  (assoc peer/peer-default-state
         :blocks
         #{finished-block unfinished-block}))

(deftest finished-pieces-test
  (is (= #{finished-block}
         (peer/finished-pieces 3 finished-peer))))

(deftest finished-pieces-test
  (is (= {:blocks #{unfinished-block}
          :pending-verify #{finished-block}}
         (peer/collect-finished-pieces
           3
           {:blocks #{finished-block unfinished-block}
            :pending-verify #{}}))))
