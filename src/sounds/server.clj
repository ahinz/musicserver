(ns sounds.server
  (:use
   [clojure.java.shell :only [sh]])
  (:require
   [lamina.core :as lamina]))

(def download-dir "/tmp/downloads/")
(def mplayer-bin "/usr/local/bin/mplayer")

(defn get-next-song []
  "/Users/ahinz/Music/iTunes/iTunes Music/Moulin Rouge/Moulin Rouge/06 Rhythm of the Night.mp3")

(def download-channel (lamina/channel))
(def play-channel (lamina/channel))

(defn loop-forever [f]
  (doall (repeatedly f))) ; Since repeatedly is lazy, we wrap it in doall

(defn play-song-mplayer [s]
  (:exit (sh mplayer-bin "-af" "volume=15" s)))

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
           song (get-next-song)]
       (println "[Downloader] Queue song:" song)
       (lamina/enqueue play-channel song)))))

(defn start-server []
  (doseq [i (range 3)]
    (let [song (get-next-song)]
      (println "[Startup] Queue Song:" song)
      (lamina/enqueue play-channel song)))
  (-> (Thread. downloader) .start)
  (-> (Thread. player) .start))
