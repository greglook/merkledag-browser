(ns merkledag.browser.db
  "Database code for the browser state."
  (:require
    [cljs.reader]
    [multihash.core :refer [Multihash]]
    [schema.core :as s :refer-macros [defschema]]))


(def initial-value
  {:server-url "http://localhost:8080"
   ; TODO: authentication credentials
   :view/show :home
   :view/state {}
   ; TODO: make this an LRU cache of multihash -> node value
   :nodes {}})



;; ## Database Schema

(defrecord MerkleLink
  [target name tsize])


(defschema LinkSchema
  "Named link to a node in the merkledag."
  {:target Multihash
   :name (s/maybe s/Str)
   (s/optional-key :tsize) s/Int})


(defschema NodeInfo
  "Information about a stored node in the merkledag."
  {:id Multihash
   :size s/Num
   (s/optional-key :stored-at) js/Date
   (s/optional-key :content) js/Uint8Array
   (s/optional-key :encoding) [s/Str]
   (s/optional-key :links) [LinkSchema]
   (s/optional-key :data) s/Any
   s/Keyword s/Any})


(defschema ViewKeyword
  (s/enum :home
          :blocks-list
          :node-detail
          :refs-list
          :ref-detail))


(defschema DatabaseSchema
  {:server-url s/Str
   :view/show ViewKeyword
   :view/state {ViewKeyword (s/maybe {s/Keyword s/Any})}
   (s/optional-key :view/loading) s/Bool
   (s/optional-key :view/counter) s/Int
   :nodes {Multihash NodeInfo}})



;; ## Operating Functions

(defn update-node
  "Merges the given map into the database record for a block."
  [db id data]
  (update-in db [:nodes id] merge data))



;; ## Persistence

; TODO: LocalStorage persistence code
