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



;; ## Configuration Handlers

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



;; ## View Handlers

(defn- on-node-detail
  "Handles the db and dispatches when the view is changed to show node details."
  [db new-state]
  (let [current-id (get-in db [:view/state :node-detail :id])
        new-id (:id new-state)]
    (if (and new-id (not= new-id current-id))
      (do (dispatch [:load-node! new-id])
          (update-in db [:view/state :node-detail] dissoc :raw-content))
      db)))


;; Set which view is showing in the interface.
(register-handler :show-view
  [check-db! trim-v]
  (fn [db [view state]]
    (-> (case view
          :node-detail (on-node-detail db state)
          db)
        (update-in [:view/state view] merge state)
        (assoc :view/show view))))


;; Update the state for the given view.
(register-handler :update-view
  [check-db! trim-v]
  (fn [db [view state]]
    (update-in db [:view/state view] merge state)))


(register-handler :report-error
  [trim-v]
  (fn [db [tag err]]
    (println "Error talking to server in" tag err)
    (assoc db :view/loading false)))



;; ## Block Server Handlers

(register-handler :scan-blocks!
  [check-db! trim-v]
  (fn [db _]
    (println "Scanning new blocks")
    (ajax/GET (str (:server-url db) "/blocks/")
      {:response-format (edn-response-format)
       :handler #(dispatch [:update-blocks %])
       :error-handler #(dispatch [:report-error :scan-blocks! %])})
    (assoc db :view/loading true)))


(register-handler :update-blocks
  [check-db! trim-v]
  (fn [db [response]]
    (println "Successfully fetched blocks")
    (-> (reduce db/update-node db (:items response))
        (assoc :view/loading false))))


(register-handler :load-block-content!
  [check-db! trim-v]
  (fn [db [id]]
    (println "Loading block" (str id))
    (ajax/GET (str (:server-url db) "/blocks/" (multihash/base58 id))
      {:handler #(dispatch [:update-view :node-detail {:raw-content (str->bytes %)}])
       :error-handler #(dispatch [:report-error :load-block-content! %])})
    (assoc db :view/loading true)))



;; ## Node Server Handlers

(register-handler :load-node!
  [check-db! trim-v]
  (fn [db [id force?]]
    (if (or force? (nil? (get-in db [:nodes id ::loaded])))
      (do (println "Loading node" (str id))
          (ajax/GET (str (:server-url db) "/nodes/" (multihash/base58 id)
                         "?t=" (js/Date.)) ; FIXME: ugh, this is gross
            {:response-format (edn-response-format)
             :handler #(dispatch [:update-node id (assoc % ::loaded (js/Date.))])
             :error-handler #(dispatch [:report-error :load-node! %])})
          (assoc db :view/loading true))
      (do (println "Using cached node" (str id))
          db))))


(register-handler :update-node
  [check-db! trim-v]
  (fn [db [id data]]
    (println "Updating node" (str id) "with" data)
    (-> db
        (db/update-node (assoc data :id id))
        (assoc :view/loading false))))



;; ## Ref Server Handlers

(register-handler :fetch-refs!
  [check-db! trim-v]
  (fn [db _]
    (println "Fetching current refs")
    (ajax/GET (str (:server-url db) "/refs/"
                   "?t=" (js/Date.)) ; FIXME: ugh, this is gross
      {:response-format (edn-response-format)
       :handler #(dispatch [:update-refs %])
       :error-handler #(dispatch [:report-error :fetch-refs! %])})
    (assoc db :view/loading true)))


(register-handler :update-refs
  [check-db! trim-v]
  (fn [db [response]]
    (println "Successfully fetched " (count (:items response)) " refs")
    (assoc db
      :refs (into {} (map (juxt :name #(dissoc % :href)) (:items response)))
      :view/loading false)))


(register-handler :pin-ref
  [check-db! trim-v]
  (fn [db [ref-name value]]
    (println "Updating ref" ref-name "to" (pr-str value))
    (update db :pinned-refs (if value conj disj) ref-name)))
