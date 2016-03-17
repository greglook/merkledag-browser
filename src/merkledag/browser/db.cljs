(ns merkledag.browser.db
  "Database code for the browser state."
  (:require
    [cljs.reader]
    [multihash.core :refer [Multihash]]
    [schema.core :as s :refer-macros [defschema]]))


(def initial-value
  {; Persistent app configuration.
   :server-url "http://localhost:8080"
   :pinned-refs #{}

   ; Transient view state.
   :view/show :home
   :view/state {}
   :view/loading false
   :view/counter 0

   ; Local repo representation.
   :nodes {}
   :refs {}})



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


(defschema RefVersion
  {:name s/Str
   :value Multihash
   :version s/Int
   :time js/Date})


(defschema ViewKeyword
  (s/enum :home
          :blocks-list
          :node-detail
          :refs-list
          :ref-detail))


(defschema DatabaseSchema
  {:server-url s/Str
   :pinned-refs #{s/Str}
   :view/show ViewKeyword
   :view/state {ViewKeyword (s/maybe {s/Keyword s/Any})}
   :view/loading s/Bool
   :view/counter s/Int
   :nodes {Multihash NodeInfo}
   :refs {s/Str RefVersion}})



;; ## Operating Functions

(defn update-node
  "Merges the given map into the database record for a block."
  [db id data]
  (update-in db [:nodes id] merge data))



;; ## Persistence

; TODO: LocalStorage persistence code
