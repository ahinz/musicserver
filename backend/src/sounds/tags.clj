(ns sounds.tags
  (:require
   [clojure.data.json :as json]
   [clojure.string :as s])
  (:import [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey]))

(defn tags [file]
  (let [fields (apply conj {} (map (fn [n] [(keyword (. (. n toString) toLowerCase)) n]) (. FieldKey values)))
        tag (. file (getTag))]
    (apply conj {}
           (filter (fn [[name val]] (and val (not (empty? val))))
                   (map (fn [[name val]]
                          [name (seq (map #(try
                                             (. % getContent)
                                             (catch Exception e (println "Oops:" e)))
                                          (. tag (getFields val))))])
                        fields)))))

(defn audioheader [file]
   (bean (.getAudioHeader file)))

(defn- url-encode [u]
  (java.net.URLEncoder/encode u))

(defn- lookup-by-term [entity attr term aid]
  (println (str "https://itunes.apple.com/search?entity=" entity
                "&attribute=" attr
                "&term=" (url-encode term)))
  (first
   (filter #(= aid (get % "artistId"))
           (get
            (json/read-str
             (slurp
              (str "https://itunes.apple.com/search?entity=" entity
                   "&attribute=" attr
                   "&term=" (url-encode term))))
            "results"))))

(defn- grab-artwork-link [record]
  (s/replace (last
              (first
               (filter #(.startsWith (first %) "artworkUrl") record)))
             #"[0-9]+x[0-9]+-[0-9]+(.jpg)$" "600x600-75$1"))

(defn itunes-search-link [tags]
  (let [artist (first (or (:album_artist tags) (:artist tags)))
        album (first (:album tags))
        title (first (:title tags))]
    (if (and artist (or album title))
      (let [artists (slurp (str "https://itunes.apple.com/search?entity=musicArtist&term="
                                (url-encode artist)))
            aid (get
                 (first (get (json/read-str artists) "results"))
                 "artistId")
            record (or (and album
                            (lookup-by-term "album" "albumTerm" album aid))
                       (and title
                            (lookup-by-term "song" "songTerm" title aid)))]
        (and record {:art (grab-artwork-link record)
                     :url (get record "collectionViewUrl")})))))

(defn metadata [filename]
  (let [file (AudioFileIO/read (java.io.File. filename))
        t (tags file)]
    {:tags (tags file)
     :refs (itunes-search-link t)}))
