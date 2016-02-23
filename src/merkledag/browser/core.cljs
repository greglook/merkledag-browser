(ns merkledag.browser.core
  (:require-macros
    [secretary.core :refer [defroute]])
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [reagent.core :as reagent]
    [secretary.core :as secretary])
  (:import
    goog.History))


(defonce app-state
  (reagent/atom
    {:server-url "http://localhost:8080"
     :view [:home]
     :blocks []}))


(enable-console-print!)

#_
(secretary/set-config! :prefix "#")


(def history
  "Hook into browser navigation events."
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))


(defroute "/" []
  (dispatch [:show-view :home]))

(defroute "/node/:id" [id]
  ; TODO: parse id as a multihash
  (dispatch [:show-view :node id]))

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


(defn show-node-view
  [id]
  ; TODO: fire off ajax request here?
  [:div
   [:h1 id]
   [:a {:href "#/"} "Home"]])


(defn current-page-view
  []
  (let [view (:view @app-state)]
    (case (first view)
      :home [list-blocks-view]
      :node/show [show-node-view (second view)]
      [:div
       [:h1 "???"]
       [:p "Unknown view: " [:code (pr-str view)]]])))



(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)


(defn ^:export main
  []
  (define-app-routes!)
  (hook-browser-navigation!)
  (reagent/render-component
    [current-page-view]
    (. js/document (getElementById "app"))))


(defonce setup (main))
