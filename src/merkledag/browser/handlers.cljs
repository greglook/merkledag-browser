(ns merkledag.browser.handlers
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [merkledag.browser.db :as db]
    [re-frame.core :refer [after path register-handler trim-v]]))


;; Initialize the database on application startup.
(register-handler :initialize-db
  (fn [_ _]
    db/initial-value))


;; Set which view is showing in the interface.
(register-handler :show-view
  [(path :show) trim-v]
  (fn [_ new-view]
    new-view))
