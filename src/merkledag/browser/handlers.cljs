(ns merkledag.browser.handlers
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [cljs.reader :as reader]
    [merkledag.browser.db :as db]
    [multihash.core :as multihash]
    [re-frame.core :refer [after dispatch path register-handler trim-v]]))


(reader/register-tag-parser! 'data/hash multihash/decode)


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
       :handler #(dispatch [:update-blocks true %])
       :error-handler #(dispatch [:update-blocks false %])})
    (assoc db :updating-blocks? true)))


(register-handler :update-blocks
  [trim-v]
  (fn [db [success? response]]
    (if success?
      (do (println "Successfully fetched blocks")
          (assoc db
            :blocks (into (:blocks db) (map #(vector (:id %) %) (:items response)))
            :updating-blocks? false))
      (do (println "Error updating blocks:" response)
          (assoc db
            :updating-blocks? false)))))
