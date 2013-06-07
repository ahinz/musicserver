(ns sounds.socketio
  (:import
   [com.corundumstudio.socketio Configuration SocketIOServer])
  (:require
   [lamina.core :as lamina]
   [clojure.data.json :as json]))

(set! *warn-on-reflection* true)

(defn create-push-only-server [hostname port msgqueue]
  (let [config (doto (Configuration.)
                 (.setHostname hostname)
                 (.setPort port))
        server (doto (SocketIOServer. config)
                 .start)]
    (-> (Thread.
         (fn []
           (doall (repeatedly
                   (fn []
                     (let [msg @(lamina/read-channel msgqueue)]
                       (-> server
                           .getBroadcastOperations
                           (.sendEvent "newsong" (json/write-str msg)))))))))
        .start)))
