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


(register-sub :view-state
  (fn [db [_ view]]
    (reaction (get-in @db [:view-state view]))))


(register-sub :nodes
  (fn [db _]
    (reaction (:nodes @db))))


(register-sub :node-info
  (fn [db [_ path]]
    (reaction (let [id (get-in @db (cons :view-state path))]
                (get-in @db [:nodes id])))))
