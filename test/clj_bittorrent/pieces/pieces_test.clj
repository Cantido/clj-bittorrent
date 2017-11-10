(ns clj-bittorrent.pieces.pieces-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.pieces.pieces :as pieces]))

(def info [1 2 3])

(def info-hash
  (byte-array
    [112 55 -128 113 -104
     -62 42 125 43 8
     7 55 29 118 55
     121 -88 79 -33 -49]))

(def piece-zero-good
  {:index 0
   :contents info})

(def piece-zero-bad
  {:index 0
   :contents [4 5 6]})

(def metainfo
  {:pieces [info-hash]})

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

(deftest valid-piece?-test
  (is (= true
         (pieces/valid-piece?
           {:pieces [info-hash]}
           {:index 0
            :contents info})))
  (is (= false
         (pieces/valid-piece?
           {:pieces [info-hash]}
           {:index 0
            :contents [4 5 6]})))
  (is (= false
         (pieces/valid-piece?
           {:pieces [5 6 7]}
           {:index 0
            :contents info}))))

(deftest valid-pieces-test
  (is (= #{piece-zero-good}
         (pieces/valid-pieces
           metainfo
           #{piece-zero-good})))

  (is (= #{}
         (pieces/valid-pieces
           metainfo
           #{piece-zero-bad})))

  (is (= #{}
         (pieces/valid-pieces metainfo #{}))))

(deftest invalid-pieces-test
  (is (= #{}
         (pieces/invalid-pieces
           metainfo
           #{piece-zero-good})))

  (is (= #{piece-zero-bad}
         (pieces/invalid-pieces
           metainfo
           #{piece-zero-bad})))
  (is (= #{}
         (pieces/invalid-pieces
           {:pieces [info-hash]}
           #{}))))

(deftest validate-test
  (is (= [#{} #{}]
         (pieces/validate metainfo #{})))

  (is (= [#{piece-zero-good}
          #{}]
         (pieces/validate
           metainfo
           #{piece-zero-good})))

  (is (= [#{}
          #{piece-zero-bad}]
         (pieces/validate
           metainfo
           #{piece-zero-bad})))

  (is (= [#{piece-zero-good}
          #{piece-zero-bad}]
         (pieces/validate
           metainfo
           #{piece-zero-good
             piece-zero-bad}))))
