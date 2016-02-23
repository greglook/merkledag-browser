(ns merkledag.browser.db
  "Database code for the browser state."
  (:require
    [cljs.reader]
    #_[schema.core :as s :include-macros true]))


(def initial-value
  {:server-url "http://localhost:8080"
   :node-stats (sorted-map)
   :show [:home]})



;; ## Database Schema

; ...



;; ## Persistence

; TODO: LocalStorage persistence code
