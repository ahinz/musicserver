(ns soundui.soundui
  (:require-macros [dommy.macros :as d]) ;[sel sel1 node deftemplate]
  (:require [dommy.core :as dommy]
            [clojure.browser.net :as net]
            [clojure.browser.event :as gevent]))

(d/deftemplate song-template [song]
  (let [meta (get song "meta")
        tags (get meta "tags")
        refs (get meta "refs")
        artist (or (first (get tags "artist")) (first (get tags "album_artist")))
        album (first (get tags "album"))
        title (first (get tags "title"))]
    [:div {:class "song well"}
     [:div {:class "album-art"}
      [:a {:href (get refs "url")}
        [:img {:src (get refs "art") }]]]
     [:div {:class "song-info"}
      [:table {:class "table"}
        [:tr [:th "Artist"] [:th "Song"] [:th "Album"]]
        (if (or artist album title)
          [:tr
           [:td artist]
           [:td title]
           [:td album]]
          [:tr
           [:td {:colspan 3} (get song "song")]])]
      [:table {:class "table"}
       [:tr [:th "Client"]]
       [:tr [:td (get song "client")]]]]
     [:div {:style "clear: both"}]]))

(defn ^:export init []
  (def xhr (net/xhr-connection))
  (gevent/listen xhr :complete #(.log js/console "Received"
                                      (let [div (d/sel1 :#songs)]
                                        (doall
                                         (map (comp (partial dommy/append! div)
                                                    song-template
                                                    js->clj)
                                              (-> % .-target .getResponseJson))))))

  (net/transmit xhr "http://localhost:3131/history?n=100"))

(set! (.-onload js/window) init)
