(ns merkledag.browser.handlers
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [merkledag.browser.db :as db]
    [re-frame.core :refer [after dispatch path register-handler trim-v]]))


;; Initialize the database on application startup.
(register-handler :initialize-db
  (fn [_ _]
    ;(dispatch [:scan-blocks])
    db/initial-value))


;; Set which view is showing in the interface.
(register-handler :show-view
  [(path :show) trim-v]
  (fn [_ new-view]
    new-view))


(register-handler :scan-blocks
  [trim-v]
  (fn [db _]
    (println "Scanning new blocks")
    (ajax/GET (str (:server-url db) "/blocks/")
      {:response-format (edn-response-format)
       :handler #(dispatch [:update-blocks (:entries %)])
       :error-handler #(println (str "Failed to query blocks: " (pr-str %)))})
    (assoc db :updating-blocks? true)))


(register-handler :update-blocks
  [trim-v]
  (fn [db [entries]]
    (println "Updating blocks")
    (assoc db
      :blocks entries
      :updating-blocks? false)))
