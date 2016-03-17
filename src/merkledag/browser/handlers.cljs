(ns merkledag.browser.handlers
  (:require
    [ajax.core :as ajax]
    [ajax.edn :refer [edn-response-format]]
    [alphabase.bytes :as bytes]
    [cljs.reader :as reader]
    [merkledag.browser.db :as db]
    [multihash.core :as multihash]
    [re-frame.core :refer [after dispatch path register-handler trim-v]]
    [schema.core :as s]))


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


(defn check-and-throw!
  "Throws an exception if db value doesn't match the schema."
  [schema db]
  (if-let [problems (s/check schema db)]
    (throw (ex-info (str "Schema check failed: " problems)
                    {:errors problems}))))


;; After an event handler has run, this middleware can check that
;; it the value in app-db still correctly matches the schema.
(def check-db!
  (after (partial check-and-throw! db/DatabaseSchema)))



;; ## Handlers

;; Initialize the database on application startup.
(register-handler :initialize-db
  (fn [_ _]
    (dispatch [:scan-blocks!])
    (dispatch [:fetch-refs!])
    db/initial-value))


(register-handler :touch-ui
  [check-db!]
  (fn [db _]
    (update db :view/counter (fnil inc 0))))


(register-handler :set-server-url
  [check-db! (path :server-url) trim-v]
  (fn [old-url [new-url]]
    (println "Changing server-url to:" new-url)
    new-url))


;; Set which view is showing in the interface.
(register-handler :show-view
  [check-db! trim-v]
  (fn [db [view state]]
    (case view
      :node-detail (dispatch [:load-node! (:id state)])
      nil)
    (-> db
        (assoc :view/show view)
        (update-in [:view/state view] merge state))))


(register-handler :scan-blocks!
  [check-db! trim-v]
  (fn [db _]
    (println "Scanning new blocks")
    (ajax/GET (str (:server-url db) "/blocks/")
      {:response-format (edn-response-format)
       :handler #(dispatch [:update-blocks true %])
       :error-handler #(dispatch [:update-blocks false %])})
    (assoc db :view/loading true)))


(register-handler :update-blocks
  [check-db! trim-v]
  (fn [db [success? response]]
    (if success?
      (do (println "Successfully fetched blocks")
          (-> (reduce #(db/update-node %1 (:id %2) %2) db (:items response))
              (assoc :view/loading false)))
      (do (println "Error updating blocks:" response)
          (assoc db
            :view/loading false)))))


(register-handler :load-block-content!
  [check-db! trim-v]
  (fn [db [id]]
    (println "Loading block" (str id))
    (ajax/GET (str (:server-url db) "/blocks/" (multihash/base58 id))
      {:handler #(dispatch [:update-node id true {:content (str->bytes %)}])
       :error-handler #(dispatch [:update-node id false %])})
    (assoc db :view/loading true)))


(register-handler :load-node!
  [check-db! trim-v]
  (fn [db [id force?]]
    (if (or force? (nil? (get-in db [:nodes id ::loaded])))
      (do (println "Loading node" (str id))
          (ajax/GET (str (:server-url db) "/nodes/" (multihash/base58 id)
                         "?t=" (js/Date.)) ; FIXME: ugh, this is gross
            {:response-format (edn-response-format)
             :handler #(dispatch [:update-node id true (assoc % ::loaded (js/Date.))])
             :error-handler #(dispatch [:update-node id false %])})
          (assoc db :view/loading true))
      (do (println "Using cached node" (str id))
          db))))


(register-handler :update-node
  [check-db! trim-v]
  (fn [db [id success? data]]
    (if success?
      (do (println "Updating node" (str id) "with" data)
          (-> db
              (db/update-node id data)
              (assoc :view/loading false)))
      (do (println "Failed to fetch block" (str id) ":" data)
          (assoc db :view/loading false)))))



(register-handler :fetch-refs!
  [check-db! trim-v]
  (fn [db _]
    (println "Fetching current refs")
    (ajax/GET (str (:server-url db) "/refs/"
                   "?t=" (js/Date.)) ; FIXME: ugh, this is gross
      {:response-format (edn-response-format)
       :handler #(dispatch [:update-refs true %])
       :error-handler #(dispatch [:update-refs false %])})
    (assoc db :view/loading true)))


(register-handler :update-refs
  [check-db! trim-v]
  (fn [db [success? response]]
    (if success?
      (do (println "Successfully fetched " (count (:items response)) " refs")
          (assoc db
            :refs (into {} (map (juxt :name #(dissoc % :href)) (:items response)))
            :view/loading false))
      (do (println "Error fetching refs:" response)
          (assoc db
            :view/loading false)))))


(register-handler :pin-ref
  [check-db! trim-v]
  (fn [db [ref-name value]]
    (println "Updating ref" ref-name "to" (pr-str value))
    (if value
      (update db :pinned-refs conj ref-name)
      (update db :pinned-refs disj ref-name))))
