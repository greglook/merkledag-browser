(ns merkledag.browser.views
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [re-frame.core :refer [dispatch subscribe]]))


(defn block-list
  [blocks]
  [:ul
   (for [block blocks]
     ^{:key (:id block)}
     [:li [:strong [:a {:href (str "#/node/" (:id block))} (str (:id block))]]
      " " [:span "(" (:size block) " bytes)"]])])


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
   [:h1 id]
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
