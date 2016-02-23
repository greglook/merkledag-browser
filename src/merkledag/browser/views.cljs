(ns merkledag.browser.views
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [re-frame.core :refer [dispatch subscribe]]))


#_
(defn block-list
  [blocks]
  [:ul
   (for [block blocks]
     ^{:key (:id block)}
     [:li [:strong [:a {:href (str "#/node/" (:id block))} (str (:id block))]]
      " " [:span "(" (:size block) " bytes)"]])])


#_
(defn list-blocks-view
  []
  [:div
   [:h1 "Blocks"]
   [:input {:type "button" :value "Refresh"
            :on-click (fn refresh-blocks []
                        (ajax/GET (str (:server-url @app-state) "/blocks/")
                          {:response-format (edn-response-format)
                           :handler #(do (prn %) (swap! app-state assoc :blocks (:entries %)))
                           :error-handler #(js/alert (str "Failed to query blocks: " (pr-str %)))}))}]
   [block-list (:blocks @app-state)]])


#_
(defn show-node-view
  [id]
  ; TODO: fire off ajax request here?
  [:div
   [:h1 id]
   [:a {:href "#/"} "Home"]])


(defn browser-app
  []
  (let [show-view (subscribe [:showing])]
    (fn app-component []
      (let [[view & more] @show-view]
        (case view
          :home [:h1 "Home View"]
          :node [:h1 (first more)]
          [:h1 "Unknown View"])))))
