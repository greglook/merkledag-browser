(ns merkledag.browser.core
  (:require-macros
    [secretary.core :refer [defroute]])
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [goog.events :as events]
    [goog.history.EventType :as EventType]
    [merkledag.browser.handlers]
    [merkledag.browser.queries]
    [merkledag.browser.views :as views]
    [reagent.core :as reagent]
    [re-frame.core :refer [dispatch dispatch-sync]]
    [secretary.core :as secretary])
  (:import
    goog.History))


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


(defn ^:export run
  [container]
  (dispatch-sync [:initialize-db])
  (reagent/render-component
    [views/browser-app]
    container))


(defn on-js-reload
  []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
