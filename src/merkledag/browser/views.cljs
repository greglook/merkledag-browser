(ns merkledag.browser.views
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [alphabase.bytes :as bytes]
    [alphabase.hex :as hex]
    [clojure.string :as str]
    [multihash.core :as multihash]
    [re-frame.core :refer [dispatch subscribe]]))


(defn block-list
  [blocks]
  [:ul
   (for [[id block] blocks]
     (let [b58-id (multihash/base58 id)]
       ^{:key (str id)}
       [:li [:strong [:a {:href (str "#/node/" b58-id)} b58-id]]
        " " [:span "(" (:size block) " bytes)"]]))])


(defn list-blocks-view
  []
  (let [blocks (subscribe [:blocks])]
    (fn []
      [:div
       [:h1 "Blocks"]
       [:input {:type "button" :value "Refresh"
                :on-click #(dispatch [:scan-blocks])}]
       [block-list @blocks]])))


(defn hexedit-block
  "Returns a string reminiscent of hex-editor views, with 16 bytes shown per
  line in both hexadecimal and ascii (where printable).

  Lines will be formatted like this:

  ```
  00 01 02 03 04 05 06 07  08 09 0a 0b 0c 0d 0e 0f   ........ ........
  00 01 02 03                                        ....
  ```
  "
  [data]
  (let [hex-section #(str (str/join " " (map hex/byte->hex %))
                          (when (< (count %) 8)
                            (str/join (repeat (- 8 (count %)) "   "))))
        byte->char #(if (<= 32 % 126)
                      (.fromCharCode js/String %)
                      ".")
        ascii-section #(str/join (map byte->char %))]
    (->> (bytes/byte-seq data)
         (partition 16)
         (map (fn hexedit-line
                [line-data]
                (let [left (take 8 line-data)
                      right (drop 8 line-data)]
                  (str (hex-section left)
                       "  "
                       (hex-section right)
                       "   "
                       (ascii-section left)
                       " "
                       (ascii-section right)))))
         (str/join "\n"))))


(defn show-node-view
  [id]
  (let [blocks (subscribe [:blocks])
        content (subscribe [:block-content id])]
    (fn []
      [:div
       [:h1 (multihash/base58 id)]
       (if-let [node (get @blocks id)]
         [:div
          [:p (str id)]
          [:p [:strong "Size: "]  (:size node) " bytes"]
          [:p [:strong "Encoding: "]  (interpose ", "  (map #(vector :code %) (:encoding node)))]
          [:div.content
            [:h2 "Block Content"]
            (if-let [data @content]
              [:pre (hexedit-block data)]
              [:input {:type "button" :value "Load binary content"
                       :on-click #(dispatch [:load-block-content id])}])]
          (when (:links node)
            [:div
             [:h2 "Links"]
             [:ol (map (fn [link]
                         [:li
                          [:strong (multihash/base58 (:target link))]
                          " "
                          [:a {:href (str "#/node/" (multihash/base58 (:target link)))} (:name link)]
                          (when (:tsize link)
                            (str " (" (:tsize link) " bytes)"))])
                       (:links node))]])
          (when (:data node)
            [:div
             [:h2 "Data"]
             [:code (pr-str (:data node))]])]
         [:p "Not Found"])
       [:a {:href "#/"} "Home"]])))


(defn browser-app
  []
  (let [show-view (subscribe [:showing])]
    (fn app-component []
      (let [[view & more] @show-view]
        (case view
          :home [list-blocks-view]
          :node [show-node-view (first more)]
          [:h1 "Unknown View"])))))
