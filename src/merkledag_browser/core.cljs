(ns merkledag-browser.core
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [reagent.core :as reagent]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state
  (reagent/atom {:blocks []}))


(defn hello-world []
  [:h1 (:text @app-state)])


(defn block-list
  [blocks]
  [:ul
   (for [block blocks]
     ^{:key (:id block)}
     [:li [:strong (str (:id block))] " " [:span "(" (:size block) " bytes)"]])])


(defn list-blocks-view
  []
  [:div
   [:h1 "Blocks"]
   [:input {:type "button" :value "Refresh"
            :on-click (fn refresh-blocks []
                        (ajax/GET "http://localhost:8080/blocks/"
                          {:response-format (edn-response-format)
                           :handler #(do (prn %) (swap! app-state assoc :blocks (:entries %)))
                           :error-handler #(js/alert (str "Failed to query blocks: " (pr-str %)))}))}]
   [block-list (:blocks @app-state)]])


(reagent/render-component
  [list-blocks-view]
  (. js/document (getElementById "app")))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
