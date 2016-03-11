(ns merkledag.browser.views
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [alphabase.bytes :as bytes]
    [alphabase.hex :as hex]
    [cljs.pprint :refer [pprint]]
    [clojure.string :as str]
    [merkledag.browser.routes :refer [home-path node-path]]
    [multihash.core :as multihash]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [re-frame.core :refer [dispatch subscribe]])
  (:import
    [goog.string StringBuffer]))


(defn block-list
  [blocks]
  [:div.table-responsive
   [:table.table.table-striped.block-list
    [:thead
     [:tr
      [:th "ID"]
      [:th.ralign "Size"]
      [:th.ralign "Stored At"]]]
    [:tbody
     (for [[id block] blocks]
       (let [b58-id (multihash/base58 id)]
         ^{:key (str id)}
         [:tr
          [:td [:strong [:a {:href (node-path {:id b58-id})} b58-id]]]
          [:td.ralign (:size block)]
          [:td.ralign (str (:stored-at block))]]))]]])


(defn list-blocks-view
  []
  (let [blocks (subscribe [:nodes])]
    (fn []
      [:div
       [:h1.page-header "Blocks"]
       [block-list @blocks]
       [:input {:type "button" :value "Refresh"
                :on-click #(dispatch [:scan-blocks])}]])))


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
  (let [state (subscribe [:view-state :node])
        node-id (reaction (:id @state))
        node-info (subscribe [:node-info] [node-id])]
    (fn []
      (let [{:keys [id]} @state]
        [:div
         [:h1.page-header (multihash/base58 id)]
         (if-let [node @node-info]
           [:div.row
            [:input {:type "button", :value "Reload", :on-click #(dispatch [:load-node id true])}]
            [:p (str id)]
            [:p [:strong "Size: "]  (:size node) " bytes"]
            (when (:encoding node)
              [:p [:strong "Encoding: "]  (interpose " " (map #(vary-meta (vector :code %) assoc :key %) (:encoding node)))])
            (when (:links node)
              [:div
               [:h3 "Links"]
               [:ul
                (for [link (:links node)
                      :let [b58-target (multihash/base58 (:target link))]]
                  ^{:key (str (:name link) "|" (:target link))}
                  [:li [:strong [:a {:href (node-path {:id b58-target})} b58-target]]
                   " " (:name link) " " [:span "(" (:tsize link) " total bytes)"]])]])
            (when (:data node)
              [:div
               [:h3 "Data"]
               [:pre (let [sb (StringBuffer.)
                           out (StringBufferWriter. sb)]
                       (pprint (:data node) out)
                       (str sb))]])
            [:h2.sub-header "Block Content"]
            (if (:content node)
              [:pre (hexedit-block (:content node))]
              [:input {:type "button" :value "Load binary content"
                       :on-click #(dispatch [:load-block-content id])}])]
           [:p "Not Found"])
         [:a {:href (home-path)} "Home"]]))))


(defn server-url-input
  [props]
  (let [app-config (subscribe [:app-config])
        text (r/atom (:server-url @app-config))
        reset #(reset! text (:server-url @app-config))
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
                               13 (do (save)  (.blur (.-target %)))
                               27 (do (reset) (.blur (.-target %)))
                               nil)})])))


(defn nav-bar
  "Top navigation menu and connection settings."
  []
  [:nav.navbar.navbar-inverse.navbar-fixed-top
   [:div.container-fluid
    [:div.navbar-header
     [:a.navbar-brand {:href (home-path)} "Merkledag Browser"]]
    [:div#navbar.navbar-collapse.collapse
     [:ul.nav.navbar-nav.navbar-right
      [:li [:a {:href (home-path)} "Blocks"]]
      [:li [:a {:href "#/settings"} "Settings"]]]
     [:form.navbar-form.navbar-right
      [server-url-input {:placeholder "API server"}]]]]])


(defn browser-app
  []
  (let [show-view (subscribe [:showing])
        app-config (subscribe [:app-config])]
    (fn app-component []
      ^{:key (str "ui:" (:ui-counter @app-config 0))}
      [:div
       [nav-bar]
       [:div.container-fluid
        [:div.row
         [:div.col-sm-3.col-md-2.sidebar
          [:ul.nav.nav-sidebar
           [:li.active [:a {:href (home-path)} "Overview"]]
           [:li [:a {:href "#/"} "Blocks"]]
           [:li [:a "..."]]]
          [:ul.nav.nav-sidebar
           [:li [:a "More list"]]
           [:li [:a "..."]]]]
         [:div.col-sm-9.col-sm-offset-3.col-md-10.col-md-offset-2.main
           (case @show-view
             :home [list-blocks-view]
             :node [show-node-view]
             [:h1.page-header "Unknown View"])]]]])))
