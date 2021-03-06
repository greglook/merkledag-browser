(ns merkledag.browser.views
  (:require
    [clojure.string :as str]
    [merkledag.browser.helpers :refer [edn-block hexedit-block]]
    [merkledag.browser.routes :as route]
    [multihash.core :as multihash]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [re-frame.core :refer [dispatch subscribe]]))


;; ## Block Views

(defn block-list
  [blocks]
  [:div.table-responsive
   [:table.table.table-striped
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
          [:td [:strong [:a {:href (route/block-path {:id b58-id})} b58-id]]]
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
                :on-click #(dispatch [:scan-blocks!])}]])))


(defn block-info-view
  []
  (let [view-state (subscribe [:view-state])]
    (fn []
      (let [id (:id @view-state)]
        [:div
         [:h1.page-header (multihash/base58 id)]
         [:p "..."]]))))



;; ## Node Views

(defn- node-links
  [root base-path node]
  (when (:links node)
    [:div
     [:h3 "Links"]
     [:ul
      (for [link (:links node)
            :let [b58-target (multihash/base58 (:target link))]]
        ^{:key (str (:name link) "|" (:target link))}
        [:li [:strong [:a {:href (route/block-path {:id b58-target})} b58-target]]
         " " (if (:name link)
               [:a {:href (route/data-path root (conj base-path (:name link)))} (:name link)]
               nil)
         " " [:span "(" (:tsize link) " total bytes)"]])]]))


(defn data-view
  []
  (let [view (subscribe [:view-state :data-path])
        node-id (reaction (get-in @view [:state :id]))
        node-info (subscribe [:node-info] [node-id])]
    (fn []
      (let [id @node-id
            {:keys [root path]} (:state @view)]
        [:div
         [:h1.page-header root]
         [:code (pr-str path)]
         ; TODO: render node
         (if-let [node @node-info]
           [:div.row
            [:input {:type "button", :value "Reload", :on-click #(dispatch [:load-node! id true])}]
            [:p (str id)]
            [:p [:strong "Size: "]  (:size node) " bytes"]
            (when (:encoding node)
              [:p [:strong "Encoding: "]  (interpose " " (map #(vary-meta (vector :code %) assoc :key %) (:encoding node)))])
            [node-links root path node]
            (when (:data node)
              [:div
               [:h3 "Data"]
               (edn-block (:data node))])
            [:h2.sub-header "Block Content"]
            (if-let [content (get-in @view [:state :raw-content])]
              (hexedit-block content)
              [:input {:type "button" :value "Load binary content"
                       :on-click #(dispatch [:load-block-content! id])}])]
           [:p "Node not found in store"])]))))



;; ## Ref Views

(defn new-ref-form
  []
  (let [ref-name (r/atom "")
        ref-value (r/atom "")]
    (fn []
      [:div.row
       [:label
        "Ref Name "
        [:input {:type "text"
                 :value @ref-name
                 :placeholder "Name"
                 :on-change #(reset! ref-name (-> % .-target .-value str str/trim))}]]
       [:label
        "Value "
        [:input {:type "text"
                 :value @ref-value
                 :placeholder "Multihash ID"
                 :on-change #(reset! ref-value (-> % .-target .-value str str/trim))}]]
       [:input {:type "button"
                :value "Set Ref"
                :on-click #(do (dispatch [:set-ref! @ref-name @ref-value])
                               (reset! ref-name "")
                               (reset! ref-value ""))}]])))


(defn refs-list-view
  []
  (let [ref-list (subscribe [:ref-list])
        pinned-refs (subscribe [:ref-pins])]
    (fn []
      [:div
       [:h1.page-header "Refs"]
       [new-ref-form]
       [:div.table-responsive
        [:table.table.table-striped
         [:thead
          [:tr
           [:th "Pin"]
           [:th "Name"]
           [:th "Value"]
           [:th.ralign "Version"]
           [:th.ralign "Updated At"]]]
         [:tbody
          (let [pins @pinned-refs]
            (for [[ref-name info] @ref-list
                  :let [value (multihash/base58 (:value info))]]
              ^{:key ref-name}
              [:tr
               [:td [:input {:type "checkbox"
                             :value (str "ref-pin-" ref-name)
                             :checked (contains? pins ref-name)
                             :on-change #(dispatch [:pin-ref ref-name (-> % .-target .-checked)])}]]
               [:td [:a {:href (route/data-path ref-name)} [:strong ref-name]]]
               [:td [:a {:href (route/block-path {:id value})} value]]
               [:td.ralign [:a {:href (route/ref-path {:name ref-name})} (:version info)]]
               [:td.ralign (str (:time info))]]))]]]
       ; TODO: new-ref button
       [:input {:type "button" :value "Refresh"
                :on-click #(dispatch [:fetch-refs!])}]])))


(defn ref-detail-view
  []
  (let [view (subscribe [:view-state :ref-detail])]
    (fn []
      (let [rname (:name (:state @view))]
        [:div
         [:h1.page-header rname]
         ; TODO: ref setting, deleting
         ; TODO: show ref history
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
     [:a.navbar-brand {:href (route/home-path)} "Merkledag Browser"]]
    [:div#navbar.navbar-collapse.collapse
     [:ul.nav.navbar-nav.navbar-right
      [:li [:a {:href (route/home-path)} "Blocks"]]
      [:li [:a {:href "#/settings"} "Settings"]]]
     [:form.navbar-form.navbar-right
      [server-url-input {:placeholder "API server"}]]]]])


(defn side-bar
  "Side navigation menu."
  []
  (let [view (subscribe [:view-state])
        pinned-refs (subscribe [:ref-pins])]
    (fn []
      (let [state @view
            side-link (fn [views href text]
                        [(if (contains? views (:view state))
                           :li.active
                           :li)
                         [:a {:href href} text]])]
        [:div.col-sm-3.col-md-2.sidebar
         [:ul.nav.nav-sidebar
          (side-link #{:home} (route/home-path) "Overview")
          (side-link #{:blocks-list :block-info} (route/blocks-path) "Blocks")
          (side-link #{:refs-list :ref-detail} (route/refs-path) "Refs")]
         [:h3 "Pins"]
         [:ul.nav.nav-sidebar
          (for [ref-name (sort @pinned-refs)]
            ^{:key ref-name}
            [(if (and (= :data-path (:view state))
                      (= ref-name (get-in state [:state :root])))
               :li.active
               :li)
             [:a {:href (route/data-path ref-name)} ref-name]])]]))))


(defn browser-app
  []
  (let [view (subscribe [:view-state])]
    (fn app-component []
      ^{:key (str "ui:" (:counter @view 0))}
      [:div
       [nav-bar]
       [:div.container-fluid
        [:div.row
         [side-bar]
         [:div.col-sm-9.col-sm-offset-3.col-md-10.col-md-offset-2.main
           (case (:view @view)
             (:home :blocks-list) [blocks-list-view]
             :block-info [data-view]
             :refs-list [refs-list-view]
             :ref-detail [ref-detail-view]
             :data-path [data-view]
             [:h1.page-header "Unknown View"])]]]])))
