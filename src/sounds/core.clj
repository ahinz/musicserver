(ns sounds.core
  (:use [sounds.server :only [start-server]]
        [sounds.client :only [start-client]])
  (:gen-class))

(defn -main [& args]
  (condp = (first args)
    "server"
    (start-server (rest args))
    "client"
    (let [parsed (into {"--port" "6969" "--bind" "localhost"}
                       (map vec (partition 2 2 (rest args))))]
      (start-client (Integer/parseInt (get parsed "--port"))
                    (get parsed "--file")
                    (get parsed "--server")
                    (get parsed "--bind")))))
