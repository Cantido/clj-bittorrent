(ns clj-bittorrent.message-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.message :as msg]))

(deftest message-method-test
  (is (= 4 (count (msg/message :keep-alive))))
  (is (= 5 (count (msg/message :choke))))
  (is (= 5 (count (msg/message :unchoke))))
  (is (= 5 (count (msg/message :interested))))
  (is (= 5 (count (msg/message :not-interested))))
  (is (= 9 (count (msg/message :have 1))))
  (is (= 6 (count (msg/message :bitfield [1]))))
  (is (= 17 (count (msg/message :request 1 1 1))))
  (is (= 14 (count (msg/message :piece 1 1 [1]))))
  (is (= 17 (count (msg/message :cancel 1 1 1))))
  (is (= 7 (count (msg/message :port 1)))))


(deftest apply-msg-test
  (= {} (msg/apply-msg :keep-alive {}))
  (= {:client {:choking true :interested false}
      :peer {:choking false :interested false}}
     (msg/apply-msg
       :keep-alive
       {:client {:choking false :interested false}
        :peer {:choking false :interested false}}))
  (= {:client {:choking false :interested false}
      :peer {:choking false :interested false}}
     (msg/apply-msg
       :keep-alive
       {:client {:choking true :interested false}
        :peer {:choking false :interested false}})))
