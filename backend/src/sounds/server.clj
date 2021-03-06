(ns sounds.server
  (:use
   compojure.core
   ring.adapter.jetty
   [clojure.java.shell :only [sh]])
  (:require
   [ring.middleware.cors :as cors]
   [sounds.tags :as tags]
   [clojure.java.io :as io]
   [clj-http.client :as client]
   [compojure.route :as route]
   [compojure.handler :as handler]
   [clojure.data.json :as json]
   [sounds.socketio :as sio]
   [lamina.core :as lamina]))

(def clients (atom []))
(def history (atom '()))

(def download-dir "/tmp/downloads/")
(def mplayer-bin "/usr/local/bin/mplayer")

(def download-channel (lamina/channel))
(def play-channel (lamina/channel))
(def notice-channel (lamina/channel))

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
  (let [song (fetch-next-song url)
        song-url (str url "/path?song="
                      (java.net.URLEncoder/encode (or song "")))]
    (when song
      (let [buffer (byte-array 4000000)
            download-to (str download-dir (str (rand-int 1000000)) (.replaceAll song "/" "__"))]
       (mark-song-as-done url song)
       (with-open [input (io/input-stream song-url)
                   output (io/output-stream download-to)]
         (loop []
           (let [n (.read input buffer)]
             (when (> n 0)
               (.write output buffer 0 n)
               (recur)))))
       [download-to song-url]))))

(defn player []
  (loop-forever
   (fn []
     (let [[client dllink song] @(lamina/read-channel play-channel)]
       ; Start downloading the next song
       (lamina/enqueue download-channel "")
       (println "[Player] Starting song:" song)
       (let [song-record {:client client
                          :song song
                          :meta (tags/metadata song)
                          :dl-link dllink}]
         (swap! history (fn [hist] (take 100 (conj hist song-record))))
         (lamina/enqueue notice-channel song-record))

       (play-song-mplayer song)
       (println "[Player] Finished song:" song)
       (.delete (java.io.File. song))))))

(defn rotate-clients []
  (let [c (first @clients)]
    (if c (swap! clients (fn [clients] (conj (vec (drop 1 clients)) (first clients)))))
    c))

(defn downloader []
  (println "[Downloader] Starting....")
  (loop-forever
   (fn []
     (println "[Downloader] Running and waiting...")
     (let [last-song @(lamina/read-channel download-channel)
           client (rotate-clients)]
       (if client
         (try
           (let [[song dllink] (fetch-and-mark client)]
             (println "[Downloader] Queue song:" song)
             (if song
               (lamina/enqueue play-channel [client dllink song])
               (do
                 (println "Invalid song from client" client)
                 (Thread/sleep 2000)
                 (lamina/enqueue download-channel ""))))
           (catch Exception e
             (do
               (println "Client is acting up..." client)
               (Thread/sleep 2000)
               (lamina/enqueue download-channel ""))))

         (do
           (println "[Warn] No clients connected")
           (Thread/sleep 2000)
           (lamina/enqueue download-channel "")))))))

(defroutes app
  (POST "/add-client" {body :body}
        (let [body (json/read-str (slurp body))
              client (get body "client")]
          (swap! clients (fn [clients] (vec (distinct (concat [client] clients)))))
          (json/write-str {:status "OK"})))
  (GET "/history" {params :params}
       (json/write-str
        (take (Integer/parseInt (or (:n params) "1")) @history)))

  (route/not-found "<h1>Page not found</h1>"))

(defn start-server [base-clients]
  (println "[Startup] Clients:" base-clients)
  (swap! clients (fn [_] base-clients))

  (-> (Thread. downloader) .start)
  (-> (Thread. player) .start)

  ;; Socket IO server
  (sio/create-push-only-server "192.168.16.77" 3132 notice-channel)
  (println "[SocketIO] Started socket server")

  (lamina/enqueue download-channel "")

  ;; Start listener
  (run-jetty (-> app
                 (handler/site)
                 (cors/wrap-cors :access-control-allow-origin #".*")) {:port 3131}))
