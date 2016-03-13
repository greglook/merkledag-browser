(ns merkledag.browser.views
  (:require
    [clojure.string :as str]
    [merkledag.browser.helpers :refer [edn-block hexedit-block]]
    [merkledag.browser.routes
     :refer [home-path blocks-path node-path refs-path]]
    [multihash.core :as multihash]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [re-frame.core :refer [dispatch subscribe]]))


;; ## Block Views

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


(defn blocks-list-view
  []
  (let [blocks (subscribe [:nodes])]
    (fn []
      [:div
       [:h1.page-header "Blocks"]
       [block-list @blocks]
       [:input {:type "button" :value "Refresh"
                :on-click #(dispatch [:scan-blocks])}]])))



;; ## Node Views

(defn node-detail-view
  []
  (let [view (subscribe [:view-state])
        node-id (reaction (get-in @view [:state :id]))
        node-info (subscribe [:node-info] [node-id])]
    (fn []
      (let [id @node-id]
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
               (edn-block (:data node))])
            [:h2.sub-header "Block Content"]
            (if (:content node)
              (hexedit-block (:content node))
              [:input {:type "button" :value "Load binary content"
                       :on-click #(dispatch [:load-block-content id])}])]
           [:p "Node not found in store"])
         [:a {:href (home-path)} "Home"]]))))



;; ## Ref Views

(defn refs-list-view
  []
  (let [state (subscribe [:view-state :refs-list])]
    (fn []
      (let [_ @state]
        [:div
         [:h1.page-header "Refs"]
         ; ...
         ]))))


(defn ref-detail-view
  []
  (let [state (subscribe [:view-state :ref-detail])]
    (fn []
      (let [rname (:name @state)]
        [:div
         [:h1.page-header rname]
         ; ...
         ]))))



;; ## Application Template

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


(defn side-bar
  "Side navigation menu."
  []
  (let [view (subscribe [:view-state])]
    (fn []
      (let [side-link (fn [views href text]
                        [(if (contains? views (:view @view))
                           :li.active
                           :li)
                         [:a {:href href} text]])]
        [:div.col-sm-3.col-md-2.sidebar
         [:ul.nav.nav-sidebar
          (side-link #{:home} (home-path) "Overview")
          (side-link #{:blocks-list} (blocks-path) "Blocks")
          (side-link #{:refs-list :ref-detail} (refs-path) "Refs")]
         [:h3 "Pins"]
         [:ul.nav.nav-sidebar
          ; TODO: loop display refs
          ]]))))


(defn browser-app
  []
  (let [view (subscribe [:view-state])
        app-config (subscribe [:app-config])]
    (fn app-component []
      ^{:key (str "ui:" (:ui-counter @app-config 0))}
      [:div
       [nav-bar]
       [:div.container-fluid
        [:div.row
         [side-bar]
         [:div.col-sm-9.col-sm-offset-3.col-md-10.col-md-offset-2.main
           (case (:view @view)
             (:home :blocks-list) [blocks-list-view]
             :node-detail [node-detail-view]
             :refs-list [refs-list-view]
             :ref-detail [ref-detail-view]
             [:h1.page-header "Unknown View"])]]]])))
