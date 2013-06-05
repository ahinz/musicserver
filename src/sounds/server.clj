(ns sounds.server
  (:use
   compojure.core
   ring.adapter.jetty
   [clojure.java.shell :only [sh]])
  (:require
   [clojure.java.io :as io]
   [clj-http.client :as client]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [clojure.data.json :as json]
   [lamina.core :as lamina]))

(def download-dir "/tmp/downloads/")
(def mplayer-bin "/usr/local/bin/mplayer")

(def download-channel (lamina/channel))
(def play-channel (lamina/channel))

(defn loop-forever [f]
  (doall (repeatedly f))) ; Since repeatedly is lazy, we wrap it in doall

(defn play-song-mplayer [s]
  (:exit (sh mplayer-bin s)))

(defn fetch-next-song [url]
  (let [song (json/read-str (slurp (str url "/next?n=1")))]
    (first song)))

(defn mark-song-as-done [url song]
  (client/post (str url "/finished")
               {:body (json/write-str {:song song})}))

(defn fetch-and-mark [url]
  (let [song (fetch-next-song url)]
    (when song
      (let [buffer (byte-array 4000000)
            download-to (str download-dir (str (rand-int 1000000)) (.replaceAll song "/" "__"))]
       (mark-song-as-done url song)
       (with-open [input (io/input-stream (str url "/path?song="
                                               (java.net.URLEncoder/encode song)))
                   output (io/output-stream download-to)]
         (loop []
           (let [n (.read input buffer)]
             (when (> n 0)
               (.write output buffer 0 n)
               (recur)))))
       download-to))))

(defn player []
  (loop-forever
   (fn []
     (let [song @(lamina/read-channel play-channel)]
       ; Start downloading the next song
       (lamina/enqueue download-channel "")))))
       (println "[Player] Starting song:" song)
       (play-song-mplayer song)
       (println "[Player] Finished song:" song)
       (.delete (java.io.File. song)))

(def clients (atom []))

(defn rotate-clients []
  (let [c (first @clients)]
    (if c (swap! clients (fn [clients] (conj (vec (drop 1 clients)) (first clients)))))
    c))

(defn downloader []
  (loop-forever
   (fn []
     (let [last-song @(lamina/read-channel download-channel)
           client (rotate-clients)]
       (if client
         (let [song (fetch-and-mark client)]
           (println "[Downloader] Queue song:" song)
           (lamina/enqueue play-channel song))
         (do
           (println "[Warn] No clients connected")
           (Thread/sleep 2000)
           (lamina/enqueue download-channel "")))))))

(defroutes app
  (POST "/add-client" {body :body}
        (let [body (json/read-str (slurp body))
              client (get body "client")]
          (swap! clients (fn [clients] (vec (distinct (concat [client] clients)))))

          ;; Queue up a song from the new client
          ;; (lamina/enqueue download-channel "")
          (json/write-str {:status "OK"})))

  (route/not-found "<h1>Page not found</h1>"))

(defn start-server [base-clients]
  (println "[Startup] Clients:" base-clients)
  (swap! clients (fn [_] base-clients))

  (-> (Thread. downloader) .start)
  (-> (Thread. player) .start)

  (lamina/enqueue download-channel ""))

  ;; Start listener
  (run-jetty (handler/site app) {:port 3131}))
