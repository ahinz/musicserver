(ns sounds.server
  (:use
   [clojure.java.shell :only [sh]])
  (:require
   [clojure.java.io :as io]
   [clj-http.client :as client]
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
       (println "[Player] Starting song:" song)
       (play-song-mplayer song)
       (println "[Player] Finished song:" song)
       (lamina/enqueue download-channel "")))))

(defn downloader []
  (loop-forever
   (fn []
     (let [last-song @(lamina/read-channel download-channel)
           song (fetch-and-mark "http://localhost:10100")]
       (println "[Downloader] Queue song:" song)
       (lamina/enqueue play-channel song)))))

(defn start-server [clients]
  (println "[Startup] Clients:" clients)
  (-> (Thread. downloader) .start)
  (-> (Thread. player) .start)

  ;; Trigger 3 downloads
  (doseq [i (range 3)]
    (lamina/enqueue download-channel ""))
)
