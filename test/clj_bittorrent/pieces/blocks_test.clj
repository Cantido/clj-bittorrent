(ns clj-bittorrent.pieces.blocks-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.pieces.blocks :as blocks]))

(deftest remove-blocks-matching-indices-test
  (is (= #{}
         (blocks/remove-blocks-matching-indices
           #{{:index 1}}
           1)))
  (is (= #{{:index 2}}
         (blocks/remove-blocks-matching-indices
           #{{:index 1} {:index 2}}
           1))))

(def block-zero {:index 0 :offset 0 :contents [0]})
(def block-one  {:index 0 :offset 1 :contents [1]})
(def block-two  {:index 0 :offset 2 :contents [2]})

(def block-zero-one {:index 0 :offset 0 :contents [0 1]})
(def block-all {:index 0 :offset 0 :contents [0 1 2]})

(deftest add-block-test
  (testing "conjing a block to another"
    (testing "when the blocks are adjancent"
      (is (= #{block-zero-one}
             (blocks/conj-condense
               #{block-zero}
               block-one))))
    (testing "when the blocks are not adjacent"
      (is (= #{block-zero block-two}
             (blocks/conj-condense
               #{block-zero}
               block-two))))
    (testing "when a third block is adjacent between two existing blocks"))
  (is (= #{block-all}
         (blocks/conj-condense
           #{block-zero block-two}
           block-one))))
