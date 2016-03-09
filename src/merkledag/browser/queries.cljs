(ns merkledag.browser.queries
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [re-frame.core :refer [register-sub]]))


(register-sub :showing
  (fn [db _]
    (reaction (:show @db))))


(register-sub :app-config
  (fn [db _]
    (reaction {:server-url (:server-url @db)
               :ui-counter (:ui-counter @db 0)})))


(register-sub :nodes
  (fn [db _]
    (reaction (:nodes @db))))


(register-sub :node-info
  (fn [db [_ id]]
    (reaction (get (:nodes @db) id))))
