(ns merkledag.browser.handlers
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [alphabase.bytes :as bytes]
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


(register-handler :set-server-url
  [(path :server-url) trim-v]
  (fn [old-url [new-url]]
    (println "Changing server-url to:" new-url)
    new-url))


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


(register-handler :load-block-content
  [trim-v]
  (fn [db [id]]
    (println "Loading block" (str id))
    (ajax/GET (str (:server-url db) "/blocks/" (multihash/base58 id))
      {:handler #(dispatch [:update-block-content id true %])
       :error-handler #(dispatch [:update-block-content id false %])})
    (assoc db :updating-blocks? true)))


(defn str->bytes
  "Converts a string response into raw bytes."
  [string]
  (bytes/init-bytes
    (reduce (fn add-char-bytes
              [acc chr]
              (loop [code (.charCodeAt chr 0)
                     acc acc]
                (if (zero? code)
                  acc
                  (recur (bit-and 0xFF (bit-shift-right code 8))
                         (conj acc code)))))
            [] string)))


(register-handler :update-block-content
  [trim-v]
  (fn [db [id success? response]]
    (if success?
      (do (println "Successfully fetched block" (str id))
          (assoc db
            :block-content (assoc (:block-content db) id (str->bytes response))
            :updating-blocks? false))
      (do (println "Failed to fetch block" (str id) ":" response)
          (assoc db
            :updating-blocks? false)))))
