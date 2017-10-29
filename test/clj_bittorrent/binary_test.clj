(ns clj-bittorrent.binary-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.binary :as bin]
            [clojure.test.check.clojure-test :refer :all]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(deftest ubyte-test
  (= 239 (bin/ubyte -17)))

(deftest sbyte-test
  (= -17 (bin/sbyte 239)))

(declare reflective-spec sbyte-spec x)

(defspec reflective-spec
  (prop/for-all [x gen/byte]
    (is (= x (-> x bin/ubyte bin/sbyte)))))

(defspec sbyte-spec
  (prop/for-all [x gen/byte]
    (is (bin/sbyte? x))
    (is (bin/ubyte? (bin/ubyte x)))))
