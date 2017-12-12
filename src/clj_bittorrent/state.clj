(ns clj-bittorrent.state)

(defn fsm
  "Returns a state machine with the given transition table."
  [states]
  (fn [state transition]
    (get-in states [state transition])))
