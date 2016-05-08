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
;; the value in app-db still correctly matches the schema.
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

(defmulti on-show
  "Updates the db and dispatches events when the view is changed."
  (fn [db view state] view))


(defmethod on-show :default
  [db _ _]
  db)


(defmethod on-show :data-path
  [db view state]
  (let [current-root (get-in db [:view/state view :root])
        current-path (get-in db [:view/state view :path])
        new-root (:root state)
        new-path (:path state)]
    (if (and (= current-root new-root)
             (= current-path new-path))
      db
      (do (dispatch [:load-data! new-root new-path])
          (update-in db [:view/state view] dissoc :raw-content)))))


;; Set which view is showing in the interface.
(register-handler :show-view
  [check-db! trim-v]
  (fn [db [view state]]
    (-> db
        (on-show view state)
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

(register-handler :load-data!
  [check-db! trim-v]
  (fn [db [root path force?]]
    (if (or force? (nil? (get-in db [:nodes id ::loaded])))
      (do (println "Loading node" (str id))
          (ajax/GET (str (:server-url db) "/data/" (multihash/base58 id))
            {:params {:t (js/Date.)}
             :response-format (edn-response-format)
             :handler #(dispatch [:update-node id (assoc % ::loaded (js/Date.))])
             :error-handler #(dispatch [:report-error :load-data! %])})
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
    (ajax/GET (str (:server-url db) "/refs/")
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


(register-handler :set-ref!
  [check-db! trim-v]
  (fn [db [ref-name value]]
    (ajax/PUT (str (:server-url db) "/refs/" ref-name)
      {:body (pr-str {:value value})
       :headers {"Content-Type" "application/edn"}
       :response-format (edn-response-format)
       :handler #(dispatch [:update-refs {:items [%]}])
       :error-handler #(dispatch [:report-error :set-ref! %])})
    (assoc db :view/loading true)))


(register-handler :pin-ref
  [check-db! trim-v]
  (fn [db [ref-name value]]
    (println "Updating ref" ref-name "to" (pr-str value))
    (update db :pinned-refs (if value conj disj) ref-name)))
