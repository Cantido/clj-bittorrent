(ns clj-bittorrent.net
  (:require [schema.core :as schema]
            [clj-bittorrent.numbers :as n]))

(def Port n/NonNegativeInt)
