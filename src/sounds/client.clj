(ns sounds.client
  (:use
   compojure.core
   ring.adapter.jetty
   [clojure.string :only [join]])
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [compojure.route :as route]
   [compojure.handler :as handler]))

(def queue (atom []))

(defn enqueue [file]
  (swap! queue conj file))

(defroutes app
  (GET "/next" {params :params}
       (json/write-str (take (Integer/parseInt (or (:n params) "1")) @queue)))
  (GET "/path" {params :params}
       (let [zzz (println params)
             song (:song params)
             song-file (io/as-file song)
             zzz (println "Song" song "File" song-file)
             data (byte-array (.length song-file))]
         (with-open [input (io/input-stream song-file)]
           (.read input data)
           (java.io.ByteArrayInputStream. data))))
  (POST "/finished" {body :body}
        (let [body (json/read-str (slurp body))
              song (get body "song")]
          (swap! queue (fn [queue song]
                         (let [i (.indexOf queue song)]
                           (if (= -1 i)
                             queue
                             (vec (concat (take i queue) (drop (+ i 1) queue))))))
                 song)
          (json/write-str (take 1 @queue))))

  (route/not-found "<h1>Page not found</h1>"))

(defn start-client [port file]
  (if file
    (doseq [f (.split (slurp file) "\n")]
      (println "[Client] Adding song:" f)
      (enqueue f)))

  (run-jetty (handler/site app) {:port port}))
