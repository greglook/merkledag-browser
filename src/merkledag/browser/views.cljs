(ns merkledag.browser.views
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [alphabase.bytes :as bytes]
    [alphabase.hex :as hex]
    [clojure.string :as str]
    [multihash.core :as multihash]
    [reagent.core :as r]
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
         (partition-all 16)
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
       [:h1.page-header (multihash/base58 id)]
       (if-let [node (get @blocks id)]
         [:div.row
          [:p (str id)]
          [:p [:strong "Size: "]  (:size node) " bytes"]
          [:p [:strong "Encoding: "]  (interpose ", "  (map #(vector :code %) (:encoding node)))]
          [:h2.sub-header "Block Content"]
          (if-let [data @content]
            [:pre (hexedit-block data)]
            [:input {:type "button" :value "Load binary content"
                     :on-click #(dispatch [:load-block-content id])}])]
         [:p "Not Found"])
       [:a {:href "#/"} "Home"]])))


(defn server-url-input
  [props]
  (let [connection-info (subscribe [:connection-info])
        text (r/atom (:server-url @connection-info))
        reset #(reset! text (:server-url @connection-info))
        save #(let [url (-> @text str str/trim)]
                (when-not (empty? url)
                  (dispatch [:set-server-url url]))
                (reset! text url))]
    (fn []
      [:input.form-control
       (merge props
              {:type "text"
               :value @text
               :on-blur save
               :on-change #(reset! text (-> % .-target .-value))
               :on-key-down #(case (.-which %)
                               13 (.blur (.-target %))
                               27 (reset)
                               nil)})])))


(defn nav-bar
  "Top navigation menu and connection settings."
  []
  (let [connection-info (subscribe [:connection-info])]
    (fn nav-component []
      [:nav.navbar.navbar-inverse.navbar-fixed-top
       [:div.container-fluid
        [:div.navbar-header
         ; button?
         [:a.navbar-brand {:href "#/"} "Merkledag Browser"]]
        [:div#navbar.navbar-collapse.collapse
         [:ul.nav.navbar-nav.navbar-right
          [:li [:a {:href "#/"} "Blocks"]]
          [:li [:a {:href "#/settings"} "Settings"]]]
         [:form.navbar-form.navbar-right
          [server-url-input {:placeholder "API server"}]
          #_
          [:input.form-control
           {:type :text
            :placeholder "API server"
            :defaultValue (:server-url @connection-info)}]]]]])))


(defn browser-app
  []
  (let [show-view (subscribe [:showing])]
    (fn app-component []
      (let [[view & more] @show-view]
        [:div
         [nav-bar]
         [:div.container-fluid
          [:div.row
           [:div.col-sm-3.col-md-2.sidebar
            [:ul.nav.nav-sidebar
             [:li.active [:a {:href "#/"} "Overview"]]
             [:li [:a {:href "#/"} "Blocks"]]
             [:li [:a "..."]]]
            [:ul.nav.nav-sidebar
             [:li [:a "More list"]]
             [:li [:a "..."]]]]
           [:div.col-sm-9.col-sm-offset-3.col-md-10.col-md-offset-2.main
             (case view
               :home [list-blocks-view]
               :node [show-node-view (first more)]
               [:h1.page-header "Unknown View"])]]]]))))
