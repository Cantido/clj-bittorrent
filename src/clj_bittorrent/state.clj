(ns clj-bittorrent.state)

(defn- transition-fn
  "Returns a state machine with the given transition table."
  [states]
  (fn [state transition]
    (get-in states [state transition])))

(defn- entity-transition-fn
  "Returns a next-state function for the given transition function, which will
  update the :state key of a given entity."
  [tfn]
  (fn next-state
   [entity transition]
   {:pre [(:state entity) transition]
    :post [(:state %)]}
   (update-in entity [:state] #(tfn % transition))))

(defn entity-state-machine
  "Returns a state machine based on the :state key of an entity, based on the
  given transition table. Call the given fn with the entity (a map containing
  a :state key) and a state transition."
  [table]
  (entity-transition-fn (transition-fn table)))
