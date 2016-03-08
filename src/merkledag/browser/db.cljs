(ns merkledag.browser.db
  "Database code for the browser state."
  (:require
    [cljs.reader]
    #_[schema.core :as s :include-macros true]))


(def initial-value
  {:server-url "http://localhost:8080"
   ; TODO: authentication credentials
   :show [:home]
      #_ [:node Multihash [String]]
   ; TODO: LRU cache of multihash -> node value
   :blocks {}
   :block-content {}})



;; ## Database Schema

; ...



;; ## Persistence

; TODO: LocalStorage persistence code
