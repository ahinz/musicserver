(ns sounds.tags
  (:import [org.jaudiotagger.audio AudioFileIO]
           [org.jaudiotagger.tag FieldKey]))

(defn tags [file]
  (let [fields (apply conj {} (map (fn [n] [(keyword (. (. n toString) toLowerCase)) n]) (. FieldKey values)))
        tag (. file (getTag))]
    (apply conj {}
           (filter (fn [[name val]] (and val (not (empty? val))))
                   (map (fn [[name val]]
                          [name (seq (map #(. % getContent) (. tag (getFields val))))])
                        fields)))))

(defn audioheader [file]
   (bean (.getAudioHeader file)))

(defn metadata [filename]
  (let [file (AudioFileIO/read (java.io.File. filename))]
    {:tags (tags file)
     :audioheader (audioheader file)}))
