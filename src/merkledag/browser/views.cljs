(ns merkledag.browser.views
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
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


(defn show-node-view
  [id]
  [:div
   [:h1 (multihash/base58 id)]
   [:a {:href "#/"} "Home"]])


(defn browser-app
  []
  (let [show-view (subscribe [:showing])]
    (fn app-component []
      (let [[view & more] @show-view]
        (case view
          :home [list-blocks-view]
          :node [show-node-view (first more)]
          [:h1 "Unknown View"])))))
