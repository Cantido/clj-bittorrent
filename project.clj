(defproject clj-bittorrent "1.0.0-SNAPSHOT"
  :description "A Clojure library for BitTorrent."
  :url "https://github.com/Cantido/clj-bittorrent"
  :license {:name "GNU General Public License"
            :url "http://www.gnu.org/licenses/gpl.txt"}
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[org.clojure/test.check "0.9.0"]
                                  [fsmviz "0.1.3"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}}
  :dependencies [[clj-bencode "4.0.1"]
                 [clj-http "3.7.0"]
                 [org.clojars.cantido/clj-mmap "2.1.0"]
                 [prismatic/schema "1.1.7"]])
