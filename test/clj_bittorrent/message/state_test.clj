(ns clj-bittorrent.message.state-test
  (:require [clojure.test :refer :all]
            [clj-bittorrent.message.state :as msg]
            [schema.test :as st]
            [clj-bittorrent.peer.connection :as conn]))

(use-fixtures :once st/validate-schemas)


(defn client-with [ks v]
  (assoc-in conn/new-connection ks v))

(def choked-client
  (client-with [:client :state] :choked))

(def unchoked-client
  (client-with [:client :state] :ready))

(def interested-remote
  (client-with [:peer :state] :interested))

(def uninterested-remote
  (client-with [:peer :state] :ready))

(def peer-with-piece
  (client-with [:peer :have] #{6969}))

(def peer-with-pieces
  (client-with [:peer :have] #{6969 420 666}))

(def peer-with-requested
  (client-with [:peer :requested]
               #{{:index 6969
                  :offset 420
                  :length 666}}))

(def client-has-piece
  (client-with [:client :blocks]
               #{{:index 6969
                  :offset 420
                  :contents [0x23]}}))

(def peer-with-port
  (client-with [:peer :port] 6881))


(deftest apply-msg-test
  (is (= conn/new-connection
         (msg/apply-msg
           {:id :keep-alive}
           conn/new-connection)))

  (is (= choked-client
         (msg/apply-msg
           {:id :choke}
           unchoked-client)))

  (is (= choked-client
         (msg/apply-msg
           {:id :choke}
           choked-client)))

  (is (= unchoked-client
         (msg/apply-msg
           {:id :unchoke}
           choked-client)))

  (is (= unchoked-client
         (msg/apply-msg
           {:id :unchoke}
           unchoked-client)))

  (is (= interested-remote
         (msg/apply-msg
           {:id :interested}
           uninterested-remote)))

  (is (= interested-remote
         (msg/apply-msg
           {:id :interested}
           interested-remote)))

  (is (= uninterested-remote
         (msg/apply-msg
           {:id :not-interested}
           interested-remote)))

  (is (= uninterested-remote
         (msg/apply-msg
           {:id :not-interested}
           uninterested-remote)))

  (is (= (:have peer-with-piece)
         (:have
           (msg/apply-msg
             {:id :have
              :index 6969}
             conn/new-connection))))

  (is (= (:have peer-with-piece)
         (:have
           (msg/apply-msg
             {:id :have
              :index 6969}
             peer-with-piece))))

  (is (= (:peer (:have peer-with-piece))
         (:peer (:have
                  (msg/apply-msg
                    {:id :have
                     :index 6969}
                    peer-with-requested)))))

  (is (= (:have peer-with-pieces)
         (:have
           (msg/apply-msg
             {:id :bitfield
              :indices #{6969 420 666}}
             conn/new-connection))))

  (is (= (:have peer-with-pieces)
         (:have
           (msg/apply-msg
             {:id :bitfield
              :indices #{6969 420 666}}
             peer-with-piece))))

  (is (= (:have peer-with-pieces)
         (:have
           (msg/apply-msg
             {:id :bitfield
              :indices #{6969 420 666}}
             peer-with-piece))))

  (is (= peer-with-requested
         (msg/apply-msg
           {:id :request
            :index 6969
            :offset 420
            :length 666}
           peer-with-piece)))

  (is (= (:client client-has-piece)
         (:client (msg/apply-msg
                    {:id :piece
                     :index 6969
                     :offset 420
                     :contents [0x23]}
                    conn/new-connection))))

  (is (= (:peer conn/new-connection)
         (:peer (msg/apply-msg
                  {:id :cancel
                   :index 6969
                   :offset 420
                   :length 666}
                  peer-with-requested))))

  (is (= (:peer peer-with-port)
         (:peer (msg/apply-msg
                  {:id :port :port 6881}
                  conn/new-connection)))))
