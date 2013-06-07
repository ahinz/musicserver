(defproject sounds "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main sounds.core
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.2"]
                 [clj-http "0.7.2"]
                 [lamina "0.5.0-rc2"]
                 [compojure "1.1.5"]
                 [ring/ring-core "1.1.8"]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [ring-cors "0.1.0"]
                 [org/jaudiotagger "2.0.3"]
                 [com.corundumstudio.socketio/netty-socketio "1.0.0"]
                 [org.slf4j/slf4j-simple "1.6.6"]])
