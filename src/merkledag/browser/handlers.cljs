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
(reader/register-tag-parser! 'data/link (partial apply db/->MerkleLink))


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



;; Initialize the database on application startup.
(register-handler :initialize-db
  (fn [_ _]
    ;(dispatch [:scan-blocks])
    db/initial-value))


(register-handler :touch-ui
  (fn [db _]
    (println "Updating current ui-counter:" (:ui-counter db))
    (update db :ui-counter (fnil inc 0))))


(register-handler :set-server-url
  [(path :server-url) trim-v]
  (fn [old-url [new-url]]
    (println "Changing server-url to:" new-url)
    new-url))


;; Set which view is showing in the interface.
(register-handler :show-view
  [trim-v]
  (fn [db [view state]]
    (case view
      :node-detail (dispatch [:load-node (:id state)])
      nil)
    (-> db
        (assoc :view/show view)
        (update-in [:view/state view] merge state))))


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
          (-> (reduce #(db/update-node %1 (:id %2) %2) db (:items response))
              (assoc :updating-blocks? false)))
      (do (println "Error updating blocks:" response)
          (assoc db
            :updating-blocks? false)))))


(register-handler :load-block-content
  [trim-v]
  (fn [db [id]]
    (println "Loading block" (str id))
    (ajax/GET (str (:server-url db) "/blocks/" (multihash/base58 id))
      {:handler #(dispatch [:update-node id true {:content (str->bytes %)}])
       :error-handler #(dispatch [:update-node id false %])})
    (assoc db :updating-blocks? true)))


(register-handler :load-node
  [trim-v]
  (fn [db [id force?]]
    (if (or force? (nil? (get-in db [:nodes id ::loaded])))
      (do (println "Loading node" (str id))
          (ajax/GET (str (:server-url db) "/nodes/" (multihash/base58 id)
                         "?t=" (js/Date.)) ; FIXME: ugh, this is gross
            {:response-format (edn-response-format)
             :handler #(dispatch [:update-node id true (assoc % ::loaded (js/Date.))])
             :error-handler #(dispatch [:update-node id false %])})
          (assoc db :updating-blocks? true))
      (do (println "Using cached node" (str id))
          db))))


(register-handler :update-node
  [trim-v]
  (fn [db [id success? data]]
    (if success?
      (do (println "Updating node" (str id) "with" data)
          (-> db
              (db/update-node id data)
              (assoc :updating-blocks? false)))
      (do (println "Failed to fetch block" (str id) ":" data)
          (assoc db :updating-blocks? false)))))
